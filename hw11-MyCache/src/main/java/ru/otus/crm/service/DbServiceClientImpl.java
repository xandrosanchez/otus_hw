package ru.otus.crm.service;

import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.otus.cachehw.HwCache;
import ru.otus.cachehw.HwListener;
import ru.otus.core.repository.DataTemplate;
import ru.otus.core.sessionmanager.TransactionManager;
import ru.otus.crm.model.Client;

public class DbServiceClientImpl implements DBServiceClient {
    private static final Logger log = LoggerFactory.getLogger(DbServiceClientImpl.class);

    private final DataTemplate<Client> clientDataTemplate;
    private final TransactionManager transactionManager;
    private final HwCache<ClientId, Client> cache;

    public DbServiceClientImpl(
            TransactionManager transactionManager,
            DataTemplate<Client> clientDataTemplate,
            HwCache<ClientId, Client> cache) {
        this.transactionManager = transactionManager;
        this.clientDataTemplate = clientDataTemplate;
        this.cache = cache;
        setupCacheListener();
    }

    @Override
    public Client saveClient(Client client) {
        return transactionManager.doInTransaction(session -> {
            var clientCloned = client.clone();
            if (client.getId() == null) {
                var savedClient = clientDataTemplate.insert(session, clientCloned);
                log.info("created client: {}", clientCloned);
                // Добавляем в кэш при создании
                cache.put(new ClientId(savedClient.getId()), savedClient);
                return savedClient;
            }
            var savedClient = clientDataTemplate.update(session, clientCloned);
            log.info("updated client: {}", savedClient);
            // Обновляем в кэш при изменении
            cache.put(new ClientId(savedClient.getId()), savedClient);
            return savedClient;
        });
    }

    @Override
    public Optional<Client> getClient(long id) {
        ClientId clientId = new ClientId(id);
        Client cachedClient = cache.get(clientId);
        if (cachedClient != null) {
            log.info("retrieved client from cache, id: {}", id);
            return Optional.of(cachedClient);
        }
        return transactionManager.doInReadOnlyTransaction(session -> {
            var clientOptional = clientDataTemplate.findById(session, id);
            if (clientOptional.isPresent()) {
                Client client = clientOptional.get();
                log.info("retrieved client from database, id: {}", id);
                // Сохраняем в кэш
                cache.put(clientId, client);
                return Optional.of(client);
            }
            log.info("client not found, id: {}", id);
            return Optional.empty();
        });
    }

    @Override
    public List<Client> findAll() {
        return transactionManager.doInReadOnlyTransaction(session -> {
            var clientList = clientDataTemplate.findAll(session);
            log.info("clientList:{}", clientList);
            return clientList;
        });
    }

    private void setupCacheListener() {
        HwListener<ClientId, Client> listener = new HwListener<ClientId, Client>() {
            @Override
            public void notify(ClientId key, Client value, String action) {
                log.debug("Cache event - key: {}, action: {}, client: {}", key, action, value.getName());
            }
        };
        cache.addListener(listener);
    }
}

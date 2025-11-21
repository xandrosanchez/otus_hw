package otus.crm.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import otus.base.AbstractHibernateTest;
import ru.otus.cachehw.HwCache;
import ru.otus.cachehw.MyCache;
import ru.otus.core.repository.DataTemplateHibernate;
import ru.otus.crm.model.Address;
import ru.otus.crm.model.Client;
import ru.otus.crm.model.Phone;
import ru.otus.crm.service.ClientId;
import ru.otus.crm.service.DbServiceClientImpl;

@DisplayName("Демо работы с hibernate (с абстракциями) должно ")
@SuppressWarnings("java:S125")
class DbServiceClientTest extends AbstractHibernateTest {

    private static final Logger log = LoggerFactory.getLogger(DbServiceClientTest.class);

    @Test
    @DisplayName(" корректно сохранять, изменять и загружать клиента")
    void shouldCorrectSaveClient() {
        // given
        var client = new Client(
                null,
                "Vasya",
                new Address(null, "AnyStreet"),
                List.of(new Phone(null, "13-555-22"), new Phone(null, "14-666-333")));

        // when
        var savedClient = dbServiceClient.saveClient(client);
        System.out.println(savedClient);

        // then
        var loadedSavedClient = dbServiceClient.getClient(savedClient.getId());
        assertThat(loadedSavedClient)
                .isPresent()
                .get()
                .usingRecursiveComparison()
                .isEqualTo(savedClient);

        // when
        var savedClientUpdated = loadedSavedClient.get();
        savedClientUpdated.setName("updatedName");
        dbServiceClient.saveClient(savedClientUpdated);

        // then
        var loadedClient = dbServiceClient.getClient(savedClientUpdated.getId());
        assertThat(loadedClient).isPresent().get().usingRecursiveComparison().isEqualTo(savedClientUpdated);
        System.out.println(loadedClient);

        // when
        var clientList = dbServiceClient.findAll();

        // then
        assertThat(clientList).hasSize(1);
        assertThat(clientList.getFirst()).usingRecursiveComparison().isEqualTo(loadedClient.get());
    }

    @Test
    @DisplayName(" обновлять кэш при изменении клиента")
    void shouldUpdateCacheOnClientModification() {
        // given
        var client = new Client(
                null, "OriginalName", new Address(null, "OriginalStreet"), List.of(new Phone(null, "11-111-11")));

        var savedClient = dbServiceClient.saveClient(client);
        long clientId = savedClient.getId();

        HwCache<ClientId, Client> cache = new MyCache<>();
        var cachedService =
                new DbServiceClientImpl(transactionManager, new DataTemplateHibernate<>(Client.class), cache);

        // when - получаем клиента (кэшируется)
        var firstGet = cachedService.getClient(clientId);

        // when - изменяем клиента
        var clientToUpdate = firstGet.get();
        clientToUpdate.setName("UpdatedName");
        cachedService.saveClient(clientToUpdate);

        // when - получаем снова (должен быть обновленный из кэша)
        var secondGet = cachedService.getClient(clientId);

        // then - проверяем, что кэш обновился
        assertThat(secondGet).isPresent();
        assertThat(secondGet.get().getName()).isEqualTo("UpdatedName");
    }

    @Test
    @DisplayName(" корректно работать с кэшем при повторных запросах")
    void shouldUseCacheForRepeatedRequests() {
        // given
        var client = new Client(
                null, "CacheTestClient", new Address(null, "CacheStreet"), List.of(new Phone(null, "99-999-99")));

        var savedClient = dbServiceClient.saveClient(client);
        long clientId = savedClient.getId();

        // Создаем сервис с кэшем для этого теста
        HwCache<ClientId, Client> cache = new MyCache<>();
        var cachedService =
                new DbServiceClientImpl(transactionManager, new DataTemplateHibernate<>(Client.class), cache);

        // when - первый запрос (должен пойти в БД)
        long firstCallStart = System.nanoTime();
        var firstResult = cachedService.getClient(clientId);
        long firstCallTime = System.nanoTime() - firstCallStart;

        // when - второй запрос (должен взять из кэша)
        long secondCallStart = System.nanoTime();
        var secondResult = cachedService.getClient(clientId);
        long secondCallTime = System.nanoTime() - secondCallStart;

        log.info("First call (DB): {} ns", firstCallTime);
        log.info("Second call (cache): {} ns", secondCallTime);

        // then - проверяем, что кэш ускорил второй запрос
        assertThat(secondCallTime).isLessThan(firstCallTime);
        assertThat(firstResult).isPresent();
        assertThat(secondResult).isPresent();
        assertThat(firstResult.get()).usingRecursiveComparison().isEqualTo(secondResult.get());
    }
}

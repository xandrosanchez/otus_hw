import static org.assertj.core.api.Assertions.assertThat;

import org.example.core.repository.executor.DbExecutorImpl;
import org.example.core.sessionmanager.TransactionRunnerJdbc;
import org.example.crm.model.Client;
import org.example.crm.model.Manager;
import org.example.crm.service.impl.DbServiceClientImpl;
import org.example.crm.service.impl.DbServiceManagerImpl;
import org.example.mapper.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Интеграционные тесты ORM (Client + Manager)")
class IntegrationOrmTest extends BaseOrmTest {

    private DbServiceClientImpl dbServiceClient;
    private DbServiceManagerImpl dbServiceManager;

    @BeforeEach
    void setUpServices() {
        var transactionRunner = new TransactionRunnerJdbc(dataSource);
        var dbExecutor = new DbExecutorImpl();

        // Настройка для Client
        EntityClassMetaData<Client> entityClassMetaDataClient = new EntityClassMetaDataImpl<>(Client.class);
        EntitySQLMetaData entitySQLMetaDataClient = new EntitySQLMetaDataImpl<>(entityClassMetaDataClient);
        DataTemplateJdbc<Client> dataTemplateClient =
                new DataTemplateJdbc<>(dbExecutor, entitySQLMetaDataClient, entityClassMetaDataClient);
        dbServiceClient = new DbServiceClientImpl(transactionRunner, dataTemplateClient);

        // Настройка для Manager
        EntityClassMetaData<Manager> entityClassMetaDataManager = new EntityClassMetaDataImpl<>(Manager.class);
        EntitySQLMetaData entitySQLMetaDataManager = new EntitySQLMetaDataImpl<>(entityClassMetaDataManager);
        DataTemplateJdbc<Manager> dataTemplateManager =
                new DataTemplateJdbc<>(dbExecutor, entitySQLMetaDataManager, entityClassMetaDataManager);
        dbServiceManager = new DbServiceManagerImpl(transactionRunner, dataTemplateManager);
    }

    @Test
    @DisplayName("Должен работать с Client и Manager одновременно")
    void shouldWorkWithBothClientAndManager() {
        // Когда
        Client savedClient = dbServiceClient.saveClient(new Client("Integration Client"));
        Manager savedManager = dbServiceManager.saveManager(new Manager("Integration Manager"));

        // Тогда
        assertThat(savedClient.getId()).isNotNull();
        assertThat(savedManager.getNo()).isNotNull();

        Client foundClient = dbServiceClient.getClient(savedClient.getId()).orElseThrow();
        Manager foundManager = dbServiceManager.getManager(savedManager.getNo()).orElseThrow();

        assertThat(foundClient.getName()).isEqualTo("Integration Client");
        assertThat(foundManager.getLabel()).isEqualTo("Integration Manager");
    }

    @Test
    @DisplayName("Должен сохранять несколько сущностей разных типов")
    void shouldSaveMultipleEntitiesOfDifferentTypes() {
        // Когда
        Client client1 = dbServiceClient.saveClient(new Client("Client One"));
        Client client2 = dbServiceClient.saveClient(new Client("Client Two"));
        Manager manager1 = dbServiceManager.saveManager(new Manager("Manager One"));
        Manager manager2 = dbServiceManager.saveManager(new Manager("Manager Two"));

        // Тогда
        assertThat(dbServiceClient.findAll()).hasSizeGreaterThanOrEqualTo(2);
        assertThat(dbServiceManager.findAll()).hasSizeGreaterThanOrEqualTo(2);

        assertThat(dbServiceClient.getClient(client1.getId())).isPresent();
        assertThat(dbServiceClient.getClient(client2.getId())).isPresent();
        assertThat(dbServiceManager.getManager(manager1.getNo())).isPresent();
        assertThat(dbServiceManager.getManager(manager2.getNo())).isPresent();
    }
}

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.example.core.repository.executor.DbExecutorImpl;
import org.example.core.sessionmanager.TransactionRunnerJdbc;
import org.example.crm.model.Client;
import org.example.crm.model.Manager;
import org.example.crm.model.Product;
import org.example.crm.service.impl.DbServiceClientImpl;
import org.example.crm.service.impl.DbServiceManagerImpl;
import org.example.crm.service.impl.DbServiceProductImpl;
import org.example.mapper.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Тесты генерации ID")
class GeneratedValueTest extends BaseOrmTest {

    private DbServiceClientImpl dbServiceClient;
    private DbServiceManagerImpl dbServiceManager;
    private DbServiceProductImpl dbServiceProduct;

    @BeforeEach
    void setUpServices() {
        var transactionRunner = new TransactionRunnerJdbc(dataSource);
        var dbExecutor = new DbExecutorImpl();

        // Client с IDENTITY стратегией
        EntityClassMetaData<Client> clientMetaData = new EntityClassMetaDataImpl<>(Client.class);
        EntitySQLMetaData clientSqlMetaData = new EntitySQLMetaDataImpl<>(clientMetaData);
        DataTemplateJdbc<Client> clientTemplate = new DataTemplateJdbc<>(dbExecutor, clientSqlMetaData, clientMetaData);
        dbServiceClient = new DbServiceClientImpl(transactionRunner, clientTemplate);

        // Manager с SEQUENCE стратегией
        EntityClassMetaData<Manager> managerMetaData = new EntityClassMetaDataImpl<>(Manager.class);
        EntitySQLMetaData managerSqlMetaData = new EntitySQLMetaDataImpl<>(managerMetaData);
        DataTemplateJdbc<Manager> managerTemplate =
                new DataTemplateJdbc<>(dbExecutor, managerSqlMetaData, managerMetaData);
        dbServiceManager = new DbServiceManagerImpl(transactionRunner, managerTemplate);

        // Product с UUID стратегией
        EntityClassMetaData<Product> productMetaData = new EntityClassMetaDataImpl<>(Product.class);
        EntitySQLMetaData productSqlMetaData = new EntitySQLMetaDataImpl<>(productMetaData);
        DataTemplateJdbc<Product> productTemplate =
                new DataTemplateJdbc<>(dbExecutor, productSqlMetaData, productMetaData);
        dbServiceProduct = new DbServiceProductImpl(transactionRunner, productTemplate);
    }

    @Test
    @DisplayName("Должен генерировать ID для Client через IDENTITY")
    void shouldGenerateIdForClientWithIdentity() {
        // Given
        Client client1 = new Client("Client One");
        Client client2 = new Client("Client Two");

        // When
        Client saved1 = dbServiceClient.saveClient(client1);
        Client saved2 = dbServiceClient.saveClient(client2);

        // Then
        assertThat(saved1.getId()).isNotNull().isPositive();
        assertThat(saved2.getId()).isNotNull().isPositive();
        assertThat(saved2.getId()).isGreaterThan(saved1.getId());

        // Проверяем, что версия установлена
        assertThat(saved1.getVersion()).isEqualTo(0L);
        assertThat(saved2.getVersion()).isEqualTo(0L);
    }

    @Test
    @DisplayName("Должен генерировать no для Manager через SEQUENCE")
    void shouldGenerateNoForManagerWithSequence() {
        // Given
        Manager manager1 = new Manager("Manager One");
        Manager manager2 = new Manager("Manager Two");

        // When
        Manager saved1 = dbServiceManager.saveManager(manager1);
        Manager saved2 = dbServiceManager.saveManager(manager2);

        // Then
        assertThat(saved1.getNo()).isNotNull().isEqualTo(1L);
        assertThat(saved2.getNo()).isNotNull().isEqualTo(2L);
    }

    @Test
    @DisplayName("Должен генерировать UUID для Product")
    void shouldGenerateUuidForProduct() {
        // Given
        Product product1 = new Product("Laptop", 999.99);
        Product product2 = new Product("Phone", 499.99);

        // When
        Product saved1 = dbServiceProduct.saveProduct(product1);
        Product saved2 = dbServiceProduct.saveProduct(product2);

        // Then
        assertThat(saved1.getId()).isNotNull().isInstanceOf(UUID.class);
        assertThat(saved2.getId()).isNotNull().isInstanceOf(UUID.class);
        assertThat(saved1.getId()).isNotEqualTo(saved2.getId());

        // Проверяем, что можем найти по UUID
        Product found =
                dbServiceProduct.getProduct(saved1.getId()).orElseThrow(() -> new AssertionError("Product not found"));
        assertThat(found.getName()).isEqualTo("Laptop");
        assertThat(found.getPrice()).isEqualTo(999.99);
    }

    @Test
    @DisplayName("Должен сохранить Product с предустановленным UUID")
    void shouldSaveProductWithPredefinedUuid() {
        // Given
        UUID predefinedUuid = UUID.randomUUID();
        Product product = new Product(predefinedUuid, "Monitor", 299.99);

        // When
        Product saved = dbServiceProduct.saveProduct(product);

        // Then
        assertThat(saved.getId()).isEqualTo(predefinedUuid);

        Product found =
                dbServiceProduct.getProduct(predefinedUuid).orElseThrow(() -> new AssertionError("Product not found"));
        assertThat(found.getName()).isEqualTo("Monitor");
    }
}

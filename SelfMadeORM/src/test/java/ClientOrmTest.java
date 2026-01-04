import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.example.core.repository.executor.DbExecutorImpl;
import org.example.core.sessionmanager.TransactionRunnerJdbc;
import org.example.crm.model.Client;
import org.example.crm.service.impl.DbServiceClientImpl;
import org.example.mapper.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ClientOrmTest extends BaseOrmTest {

    private DbServiceClientImpl dbServiceClient;

    @BeforeEach
    void setUpServices() {
        var transactionRunner = new TransactionRunnerJdbc(dataSource);
        var dbExecutor = new DbExecutorImpl();

        EntityClassMetaData<Client> entityClassMetaDataClient = new EntityClassMetaDataImpl<>(Client.class);
        EntitySQLMetaData entitySQLMetaDataClient = new EntitySQLMetaDataImpl<>(entityClassMetaDataClient);

        // Явно указываем тип Client при создании DataTemplateJdbc
        DataTemplateJdbc<Client> dataTemplateClient =
                new DataTemplateJdbc<>(dbExecutor, entitySQLMetaDataClient, entityClassMetaDataClient);

        dbServiceClient = new DbServiceClientImpl(transactionRunner, dataTemplateClient);
    }

    @Test
    @DisplayName("Должен сохранить клиента и вернуть его с ID")
    void shouldSaveClientAndReturnWithId() {
        // Given
        Client newClient = new Client("Test Client");

        // When
        Client savedClient = dbServiceClient.saveClient(newClient);

        // Then
        assertThat(savedClient).isNotNull().satisfies(client -> {
            assertThat(client.getId()).isNotNull().isPositive();
            assertThat(client.getName()).isEqualTo("Test Client");
        });
    }

    @Test
    @DisplayName("Должен найти клиента по ID после сохранения")
    void shouldFindClientByIdAfterSaving() {
        // Given
        Client savedClient = dbServiceClient.saveClient(new Client("Client To Find"));
        Long clientId = savedClient.getId();

        // When
        Client foundClient =
                dbServiceClient.getClient(clientId).orElseThrow(() -> new AssertionError("Client not found"));

        // Then
        assertThat(foundClient)
                .isNotNull()
                .extracting(Client::getId, Client::getName)
                .containsExactly(clientId, "Client To Find");
    }

    @Test
    @DisplayName("Должен вернуть пустой Optional при поиске несуществующего клиента")
    void shouldReturnEmptyOptionalForNonExistentClient() {
        // When
        var result = dbServiceClient.getClient(999999L);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Должен обновить существующего клиента")
    void shouldUpdateExistingClient() {
        // Given
        Client originalClient = dbServiceClient.saveClient(new Client("Original Name"));
        Long clientId = originalClient.getId();

        // When
        Client updatedClient = new Client(clientId, "Updated Name");
        dbServiceClient.saveClient(updatedClient);

        // Then
        Client retrievedClient = dbServiceClient.getClient(clientId).orElseThrow();
        assertThat(retrievedClient.getName()).isEqualTo("Updated Name");
    }

    @Test
    @DisplayName("Должен найти всех клиентов")
    void shouldFindAllClients() {
        // Given
        dbServiceClient.saveClient(new Client("Client 1"));
        dbServiceClient.saveClient(new Client("Client 2"));
        dbServiceClient.saveClient(new Client("Client 3"));

        // When
        List<Client> allClients = dbServiceClient.findAll();

        // Then
        assertThat(allClients)
                .isNotNull()
                .hasSizeGreaterThanOrEqualTo(3)
                .extracting(Client::getName)
                .contains("Client 1", "Client 2", "Client 3");
    }
}

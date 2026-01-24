import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowable;

import org.example.core.repository.executor.DbExecutorImpl;
import org.example.core.sessionmanager.DataBaseOperationException;
import org.example.core.sessionmanager.TransactionRunnerJdbc;
import org.example.crm.model.Client;
import org.example.crm.service.impl.DbServiceClientImpl;
import org.example.mapper.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Тесты оптимистичной блокировки")
class OptimisticLockTest extends BaseOrmTest {

    private DbServiceClientImpl dbServiceClient;

    @BeforeEach
    void setUpServices() {
        var transactionRunner = new TransactionRunnerJdbc(dataSource);
        var dbExecutor = new DbExecutorImpl();

        EntityClassMetaData<Client> entityClassMetaDataClient = new EntityClassMetaDataImpl<>(Client.class);
        EntitySQLMetaData entitySQLMetaDataClient = new EntitySQLMetaDataImpl<>(entityClassMetaDataClient);
        DataTemplateJdbc<Client> dataTemplateClient =
                new DataTemplateJdbc<>(dbExecutor, entitySQLMetaDataClient, entityClassMetaDataClient);

        dbServiceClient = new DbServiceClientImpl(transactionRunner, dataTemplateClient);
    }

    @Test
    @DisplayName("Должен сохранить клиента с версией 0")
    void shouldSaveClientWithVersionZero() {
        // Given
        Client client = new Client("Test Client");

        // When
        Client saved = dbServiceClient.saveClient(client);

        // Then
        assertThat(saved.getVersion()).isEqualTo(0L);

        Client found = dbServiceClient.getClient(saved.getId()).orElseThrow();
        assertThat(found.getVersion()).isEqualTo(0L);
    }

    @Test
    @DisplayName("Должен увеличить версию при обновлении")
    void shouldIncrementVersionOnUpdate() {
        // Given
        Client client = dbServiceClient.saveClient(new Client("Original"));
        Long clientId = client.getId();

        // When - первое обновление
        Client update1 = new Client(clientId, "First Update", client.getVersion());
        Client saved1 = dbServiceClient.saveClient(update1);

        // Then
        assertThat(saved1.getVersion()).isEqualTo(1L);
        assertThat(saved1.getName()).isEqualTo("First Update");

        Client found1 = dbServiceClient.getClient(clientId).orElseThrow();
        assertThat(found1.getVersion()).isEqualTo(1L);

        // When - второе обновление
        Client update2 = new Client(clientId, "Second Update", saved1.getVersion());
        Client saved2 = dbServiceClient.saveClient(update2);

        // Then
        assertThat(saved2.getVersion()).isEqualTo(2L);
        assertThat(saved2.getName()).isEqualTo("Second Update");
    }

    @Test
    @DisplayName("Должен кинуть OptimisticLockException при конфликте версий")
    void shouldThrowOptimisticLockExceptionOnVersionConflict() {
        // Given
        Client client = dbServiceClient.saveClient(new Client("Original"));
        Long clientId = client.getId();

        // Когда: два разных объекта с одной версией пытаются обновить одну запись
        Client update1 = new Client(clientId, "Update 1", 0L);
        Client update2 = new Client(clientId, "Update 2", 0L);

        // Первое обновление проходит
        dbServiceClient.saveClient(update1);

        // Второе обновление должно упасть с OptimisticLockException
        Throwable thrown = catchThrowable(() -> dbServiceClient.saveClient(update2));

        // Проверяем цепочку исключений
        assertThat(thrown)
                .isInstanceOf(DataBaseOperationException.class)
                .hasMessage("exception") // Внешнее сообщение
                .hasCauseInstanceOf(OptimisticLockException.class)
                .hasStackTraceContaining("была изменена версия"); // Ищем в стектрейсе

        // Альтернативно: проверяем root cause
        Throwable rootCause = getRootCause(thrown);
        assertThat(rootCause).isInstanceOf(OptimisticLockException.class).hasMessageContaining("была изменена версия");

        // Проверяем, что в БД осталось первое обновление
        Client finalClient = dbServiceClient.getClient(clientId).orElseThrow();
        assertThat(finalClient.getName()).isEqualTo("Update 1");
        assertThat(finalClient.getVersion()).isEqualTo(1L);
    }

    private Throwable getRootCause(Throwable throwable) {
        Throwable cause = throwable;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }

    @Test
    @DisplayName("Должен корректно работать при параллельных обновлениях")
    void shouldHandleConcurrentUpdatesCorrectly() {
        // Given
        Client original = dbServiceClient.saveClient(new Client("Original"));
        Long clientId = original.getId();

        // Имитация "оптимистичного" потока: читаем, что-то делаем, потом обновляем
        Client read1 = dbServiceClient.getClient(clientId).orElseThrow();
        Client read2 = dbServiceClient.getClient(clientId).orElseThrow();

        // Оба потока читают версию 0
        assertThat(read1.getVersion()).isEqualTo(0L);
        assertThat(read2.getVersion()).isEqualTo(0L);

        // Поток 1 обновляет успешно
        read1.setName("Updated by Thread 1");
        Client saved1 = dbServiceClient.saveClient(read1);
        assertThat(saved1.getVersion()).isEqualTo(1L);

        // Поток 2 пытается обновить со старой версией (0) - должен упасть
        read2.setName("Updated by Thread 2");
        assertThatThrownBy(() -> dbServiceClient.saveClient(read2)).hasCauseInstanceOf(OptimisticLockException.class);

        // Финальное состояние
        Client finalClient = dbServiceClient.getClient(clientId).orElseThrow();
        assertThat(finalClient.getName()).isEqualTo("Updated by Thread 1");
        assertThat(finalClient.getVersion()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Должен корректно обновлять при правильной версии")
    void shouldUpdateCorrectlyWithRightVersion() {
        // Given
        Client client = dbServiceClient.saveClient(new Client("Original"));
        Long clientId = client.getId();

        // Когда: последовательные обновления с правильными версиями
        Client update1 = new Client(clientId, "Update 1", 0L);
        Client saved1 = dbServiceClient.saveClient(update1);

        Client update2 = new Client(clientId, "Update 2", saved1.getVersion());
        Client saved2 = dbServiceClient.saveClient(update2);

        Client update3 = new Client(clientId, "Update 3", saved2.getVersion());
        Client saved3 = dbServiceClient.saveClient(update3);

        // Then
        assertThat(saved1.getVersion()).isEqualTo(1L);
        assertThat(saved2.getVersion()).isEqualTo(2L);
        assertThat(saved3.getVersion()).isEqualTo(3L);

        Client finalClient = dbServiceClient.getClient(clientId).orElseThrow();
        assertThat(finalClient.getName()).isEqualTo("Update 3");
        assertThat(finalClient.getVersion()).isEqualTo(3L);
    }
}

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.example.core.repository.executor.DbExecutorImpl;
import org.example.core.sessionmanager.TransactionRunnerJdbc;
import org.example.crm.model.Manager;
import org.example.crm.service.impl.DbServiceManagerImpl;
import org.example.mapper.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ManagerOrmTest extends BaseOrmTest {

    private DbServiceManagerImpl dbServiceManager;

    @BeforeEach
    void setUpServices() {
        var transactionRunner = new TransactionRunnerJdbc(dataSource);
        var dbExecutor = new DbExecutorImpl();

        EntityClassMetaData<Manager> entityClassMetaDataManager = new EntityClassMetaDataImpl<>(Manager.class);
        EntitySQLMetaData entitySQLMetaDataManager = new EntitySQLMetaDataImpl<>(entityClassMetaDataManager);
        DataTemplateJdbc<Manager> dataTemplateManager =
                new DataTemplateJdbc<>(dbExecutor, entitySQLMetaDataManager, entityClassMetaDataManager);

        dbServiceManager = new DbServiceManagerImpl(transactionRunner, dataTemplateManager);
    }

    @Test
    @DisplayName("Должен генерировать последовательные ID для Manager через SEQUENCE")
    void shouldGenerateSequentialIdsForManager() {
        // Given
        Manager manager1 = new Manager("Manager One");
        Manager manager2 = new Manager("Manager Two");
        Manager manager3 = new Manager("Manager Three");

        // When
        Manager saved1 = dbServiceManager.saveManager(manager1);
        Manager saved2 = dbServiceManager.saveManager(manager2);
        Manager saved3 = dbServiceManager.saveManager(manager3);

        // Then
        assertThat(saved1.getNo()).isEqualTo(1L);
        assertThat(saved2.getNo()).isEqualTo(2L);
        assertThat(saved3.getNo()).isEqualTo(3L);

        // Проверяем, что все сохранены
        List<Manager> allManagers = dbServiceManager.findAll();
        assertThat(allManagers)
                .hasSizeGreaterThanOrEqualTo(3)
                .extracting(Manager::getNo)
                .contains(1L, 2L, 3L);
    }

    @Test
    @DisplayName("Должен сохранить менеджера с полем no (не id)")
    void shouldSaveManagerWithNoField() {
        // Given
        Manager newManager = new Manager("Test Manager");

        // When
        Manager savedManager = dbServiceManager.saveManager(newManager);

        // Then
        assertThat(savedManager).isNotNull().satisfies(manager -> {
            assertThat(manager.getNo()).isNotNull().isPositive();
            assertThat(manager.getLabel()).isEqualTo("Test Manager");
        });
    }

    @Test
    @DisplayName("Должен найти менеджера по no (не id)")
    void shouldFindManagerByNo() {
        // Given
        Manager savedManager = dbServiceManager.saveManager(new Manager("Manager To Find"));
        Long managerNo = savedManager.getNo();

        // When
        Manager foundManager =
                dbServiceManager.getManager(managerNo).orElseThrow(() -> new AssertionError("Manager not found"));

        // Then
        assertThat(foundManager)
                .isNotNull()
                .extracting(Manager::getNo, Manager::getLabel)
                .containsExactly(managerNo, "Manager To Find");
    }

    @Test
    @DisplayName("Должен сохранить менеджера с дополнительным полем param1")
    void shouldSaveManagerWithParam1() {
        // Given - создаем без ID (null), чтобы сработал INSERT
        Manager manager = new Manager("Manager with param");
        manager.setParam1("param value");

        // When
        Manager savedManager = dbServiceManager.saveManager(manager);

        // Then
        assertThat(savedManager)
                .extracting(Manager::getLabel, Manager::getParam1)
                .containsExactly("Manager with param", "param value");
    }

    @Test
    @DisplayName("Должен найти всех менеджеров")
    void shouldFindAllManagers() {
        // Given
        dbServiceManager.saveManager(new Manager("Manager A"));
        dbServiceManager.saveManager(new Manager("Manager B"));

        // When
        List<Manager> allManagers = dbServiceManager.findAll();

        // Then
        assertThat(allManagers)
                .hasSizeGreaterThanOrEqualTo(2)
                .extracting(Manager::getLabel)
                .contains("Manager A", "Manager B");
    }

    @Test
    @DisplayName("Должен обновить label у существующего менеджера")
    void shouldUpdateManagerLabel() {
        // Given - сначала создаем менеджера
        Manager original = dbServiceManager.saveManager(new Manager("Original Label"));
        Long managerNo = original.getNo();

        // When - обновляем существующего
        Manager updated = new Manager(managerNo, "Updated Label", original.getParam1());
        Manager saved = dbServiceManager.saveManager(updated);

        // Then
        Manager retrieved = dbServiceManager.getManager(managerNo).orElseThrow();
        assertThat(retrieved.getLabel()).isEqualTo("Updated Label");
        assertThat(saved.getNo()).isEqualTo(managerNo); // ID должен сохраниться
    }

    @Test
    @DisplayName("Должен корректно обрабатывать null в param1")
    void shouldHandleNullParam1() {
        // Given
        Manager manager = new Manager("Manager with null param");
        manager.setParam1(null);

        // When
        Manager savedManager = dbServiceManager.saveManager(manager);

        // Then
        assertThat(savedManager.getParam1()).isNull();

        Manager found = dbServiceManager.getManager(savedManager.getNo()).orElseThrow();
        assertThat(found.getParam1()).isNull();
    }
}

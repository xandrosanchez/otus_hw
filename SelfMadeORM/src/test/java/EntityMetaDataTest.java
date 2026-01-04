import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Field;
import java.util.List;
import org.example.crm.model.Client;
import org.example.crm.model.Manager;
import org.example.mapper.EntityClassMetaDataImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Тесты метаданных сущностей")
class EntityMetaDataTest {

    @Test
    @DisplayName("Должен корректно определять имя сущности")
    void shouldCorrectlyDetermineEntityName() {
        // Given
        var clientMetaData = new EntityClassMetaDataImpl<>(Client.class);
        var managerMetaData = new EntityClassMetaDataImpl<>(Manager.class);

        // When & Then
        assertThat(clientMetaData.getName()).isEqualTo("Client");
        assertThat(managerMetaData.getName()).isEqualTo("Manager");
    }

    @Test
    @DisplayName("Должен находить поле с аннотацией @Id")
    void shouldFindIdField() {
        // Given
        var clientMetaData = new EntityClassMetaDataImpl<>(Client.class);
        var managerMetaData = new EntityClassMetaDataImpl<>(Manager.class);

        // When
        Field clientIdField = clientMetaData.getIdField();
        Field managerIdField = managerMetaData.getIdField();

        // Then
        assertThat(clientIdField.getName()).isEqualTo("id");
        assertThat(managerIdField.getName()).isEqualTo("no");
    }

    @Test
    @DisplayName("Должен кидать исключение если нет поля с @Id")
    void shouldThrowExceptionWhenNoIdField() {
        // Given - создадим тестовый класс без @Id
        class NoIdEntity {
            private String name;
        }

        // When & Then - исключение должно выбрасываться при создании
        assertThatThrownBy(() -> new EntityClassMetaDataImpl<>(NoIdEntity.class))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Поле с аннотацией @Id не найдено");
    }

    @Test
    @DisplayName("Должен возвращать все поля сущности")
    void shouldReturnAllFields() {
        // Given
        var clientMetaData = new EntityClassMetaDataImpl<>(Client.class);

        // When
        List<Field> allFields = clientMetaData.getAllFields();

        // Then
        assertThat(allFields).hasSize(2).extracting(Field::getName).containsExactlyInAnyOrder("id", "name");
    }

    @Test
    @DisplayName("Должен возвращать поля без ID")
    void shouldReturnFieldsWithoutId() {
        // Given
        var clientMetaData = new EntityClassMetaDataImpl<>(Client.class);
        var managerMetaData = new EntityClassMetaDataImpl<>(Manager.class);

        // When
        List<Field> clientFieldsWithoutId = clientMetaData.getFieldsWithoutId();
        List<Field> managerFieldsWithoutId = managerMetaData.getFieldsWithoutId();

        // Then
        assertThat(clientFieldsWithoutId).hasSize(1).extracting(Field::getName).containsExactly("name");

        assertThat(managerFieldsWithoutId)
                .hasSize(2)
                .extracting(Field::getName)
                .containsExactlyInAnyOrder("label", "param1");
    }

    @Test
    @DisplayName("Должен определять стратегию генерации ID")
    void shouldDetermineGenerationStrategy() {
        // Given
        var clientMetaData = new EntityClassMetaDataImpl<>(Client.class);
        var managerMetaData = new EntityClassMetaDataImpl<>(Manager.class);

        // When
        var clientStrategy = clientMetaData.getGenerationStrategy();
        var managerStrategy = managerMetaData.getGenerationStrategy();

        // Then
        assertThat(clientStrategy).isNotNull();
        assertThat(clientStrategy.getGenerationType()).isEqualTo(org.example.GenerationType.IDENTITY);

        assertThat(managerStrategy).isNotNull();
        assertThat(managerStrategy.getGenerationType()).isEqualTo(org.example.GenerationType.SEQUENCE);
        assertThat(managerStrategy.getSequenceName()).isEqualTo("manager_seq");
    }
}

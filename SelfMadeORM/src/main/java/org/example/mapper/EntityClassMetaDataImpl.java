package org.example.mapper;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.example.GeneratedValue;
import org.example.GenerationType;
import org.example.Id;

/**
 * Реализация метаданных для сущности с поддержкой аннотации @Id и @GeneratedValue
 *
 * @param <T> тип сущности
 */
public class EntityClassMetaDataImpl<T> implements EntityClassMetaData<T> {
    private final Class<T> entityClass;
    private GenerationStrategy generationStrategy;
    private String sequenceName;
    private Field idField; // Кэшируем поле ID

    public EntityClassMetaDataImpl(Class<T> entityClass) {
        this.entityClass = entityClass;
        this.idField = findIdField(); // Находим поле ID один раз
        initGenerationStrategy();
    }

    private Field findIdField() {
        return Arrays.stream(entityClass.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Id.class))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Поле с аннотацией @Id не найдено"));
    }

    private void initGenerationStrategy() {
        if (idField == null) {
            return; // Не должно случаться, но на всякий случай
        }

        GeneratedValue generatedValue = idField.getAnnotation(GeneratedValue.class);

        if (generatedValue != null) {
            GenerationType strategy = generatedValue.strategy();
            String seqName = generatedValue.sequenceName();
            this.generationStrategy = new GenerationStrategy(strategy, seqName);

            // Если стратегия SEQUENCE, но имя последовательности не указано,
            // генерируем имя по умолчанию
            if (strategy == GenerationType.SEQUENCE && (seqName == null || seqName.isEmpty())) {
                this.sequenceName = entityClass.getSimpleName().toLowerCase() + "_seq";
            } else {
                this.sequenceName = seqName;
            }
        } else {
            // По умолчанию - IDENTITY (для обратной совместимости)
            this.generationStrategy = new GenerationStrategy(GenerationType.IDENTITY);
            this.sequenceName = "";
        }
    }

    @Override
    public String getName() {
        return entityClass.getSimpleName();
    }

    @Override
    public Constructor<T> getConstructor() throws NoSuchMethodException {
        return entityClass.getConstructor();
    }

    @Override
    public Field getIdField() {
        if (idField == null) {
            idField = findIdField();
        }
        return idField;
    }

    @Override
    public List<Field> getAllFields() {
        return new ArrayList<>(List.of(entityClass.getDeclaredFields()));
    }

    @Override
    public List<Field> getFieldsWithoutId() {
        return getAllFields().stream()
                .filter(field -> !field.isAnnotationPresent(Id.class))
                .collect(Collectors.toList());
    }

    @Override
    public GenerationStrategy getGenerationStrategy() {
        return generationStrategy;
    }

    @Override
    public String getSequenceName() {
        return sequenceName;
    }
}

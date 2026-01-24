package org.example.mapper;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.example.GeneratedValue;
import org.example.GenerationType;
import org.example.Id;
import org.example.Version;

public class EntityClassMetaDataImpl<T> implements EntityClassMetaData<T> {
    private final Class<T> entityClass;
    private GenerationStrategy generationStrategy;
    private String sequenceName;
    private Field idField;
    private Field versionField;

    public EntityClassMetaDataImpl(Class<T> entityClass) {
        this.entityClass = entityClass;
        this.idField = findIdField();
        this.versionField = findVersionField();
        initGenerationStrategy();
    }

    private Field findIdField() {
        return Arrays.stream(entityClass.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Id.class))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Поле с аннотацией @Id не найдено"));
    }

    private Field findVersionField() {
        Optional<Field> versionField = Arrays.stream(entityClass.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Version.class))
                .findFirst();
        return versionField.orElse(null);
    }

    private void initGenerationStrategy() {
        if (idField == null) {
            return;
        }

        GeneratedValue generatedValue = idField.getAnnotation(GeneratedValue.class);

        if (generatedValue != null) {
            GenerationType strategy = generatedValue.strategy();
            String seqName = generatedValue.sequenceName();
            this.generationStrategy = new GenerationStrategy(strategy, seqName);

            if (strategy == GenerationType.SEQUENCE && (seqName == null || seqName.isEmpty())) {
                this.sequenceName = entityClass.getSimpleName().toLowerCase() + "_seq";
            } else {
                this.sequenceName = seqName;
            }
        } else {
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
    public Field getVersionField() {
        return versionField;
    }

    @Override
    public boolean hasVersionField() {
        return versionField != null;
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

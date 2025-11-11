package org.example.mapper;

import java.lang.reflect.Field;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Реализация генератора SQL-запросов на основе метаданных сущности
 *
 * @param <T> тип сущности
 */
public class EntitySQLMetaDataImpl<T> implements EntitySQLMetaData {
    private final EntityClassMetaData<T> metaData;

    public EntitySQLMetaDataImpl(EntityClassMetaData<T> metaData) {
        this.metaData = metaData;
    }

    @Override
    public String getSelectAllSql() {
        String tableName = metaData.getName();
        List<String> columns = metaData.getAllFields().stream()
                .map(Field::getName)
                .collect(Collectors.toList());

        return String.format("SELECT %s FROM %s",
                String.join(", ", columns),
                tableName);
    }

    @Override
    public String getSelectByIdSql() {
        String tableName = metaData.getName();
        Field idField = metaData.getIdField();

        return String.format("SELECT * FROM %s WHERE %s = ?",
                tableName,
                idField.getName());
    }

    @Override
    public String getInsertSql() {
        String tableName = metaData.getName();
        List<Field> fields = metaData.getFieldsWithoutId();

        List<String> columns = fields.stream()
                .map(Field::getName)
                .collect(Collectors.toList());

        List<String> placeholders = fields.stream()
                .map(field -> "?")
                .collect(Collectors.toList());

        return String.format("INSERT INTO %s (%s) VALUES (%s)",
                tableName,
                String.join(", ", columns),
                String.join(", ", placeholders));
    }

    @Override
    public String getUpdateSql() {
        String tableName = metaData.getName();
        Field idField = metaData.getIdField();
        List<Field> fields = metaData.getFieldsWithoutId();

        List<String> setters = fields.stream()
                .map(field -> String.format("%s = ?", field.getName()))
                .collect(Collectors.toList());

        return String.format("UPDATE %s SET %s WHERE %s = ?",
                tableName,
                String.join(", ", setters),
                idField.getName());
    }
}
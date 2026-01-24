package org.example.mapper;

import java.lang.reflect.Field;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EntitySQLMetaDataImpl<T> implements EntitySQLMetaData {

    private static final Logger log = LoggerFactory.getLogger(EntitySQLMetaDataImpl.class);
    private final EntityClassMetaData<T> metaData;

    public EntitySQLMetaDataImpl(EntityClassMetaData<T> metaData) {
        this.metaData = metaData;
        log.debug("Created EntitySQLMetaData for {}", metaData.getName());
    }

    @Override
    public String getSelectAllSql() {
        String tableName = getTableName();
        List<String> columns =
                metaData.getAllFields().stream().map(this::getColumnName).collect(Collectors.toList());

        String sql = String.format("SELECT %s FROM %s", String.join(", ", columns), tableName);

        log.debug("Generated SELECT ALL SQL: {}", sql);
        return sql;
    }

    @Override
    public String getSelectByIdSql() {
        String tableName = getTableName();
        Field idField = metaData.getIdField();

        String sql = String.format("SELECT * FROM %s WHERE %s = ?", tableName, getColumnName(idField));

        log.debug("Generated SELECT BY ID SQL: {}", sql);
        return sql;
    }

    @Override
    public String getInsertSql() {
        String tableName = getTableName();

        GenerationStrategy strategy = metaData.getGenerationStrategy();
        List<Field> fieldsToInsert;

        if (strategy.isIdentity()) {
            // Для IDENTITY не включаем ID, но включаем версию (если есть)
            fieldsToInsert = metaData.getFieldsWithoutId();
        } else {
            // Для других стратегий включаем все поля
            fieldsToInsert = metaData.getAllFields();
        }

        List<String> columns = fieldsToInsert.stream().map(this::getColumnName).collect(Collectors.toList());

        List<String> placeholders = fieldsToInsert.stream().map(field -> "?").collect(Collectors.toList());

        String sql = String.format(
                "INSERT INTO %s (%s) VALUES (%s)",
                tableName, String.join(", ", columns), String.join(", ", placeholders));

        log.debug("Generated INSERT SQL: {}", sql);
        return sql;
    }

    @Override
    public String getUpdateSql() {
        String tableName = getTableName();
        Field idField = metaData.getIdField();

        // Получаем все поля без ID
        List<Field> fields = metaData.getFieldsWithoutId();

        // Если есть поле версии, добавляем его в SET и WHERE
        if (metaData.hasVersionField()) {
            Field versionField = metaData.getVersionField();

            // Создаем SET часть: field1 = ?, field2 = ?, version = version + 1
            List<String> setters = fields.stream()
                    .filter(field -> !field.equals(versionField)) // исключаем версию из обычных SET
                    .map(field -> String.format("%s = ?", getColumnName(field)))
                    .collect(Collectors.toList());

            // Добавляем инкремент версии
            setters.add(String.format("%s = %s + 1", getColumnName(versionField), getColumnName(versionField)));

            // WHERE: id = ? AND version = ?
            String sql = String.format(
                    "UPDATE %s SET %s WHERE %s = ? AND %s = ?",
                    tableName, String.join(", ", setters), getColumnName(idField), getColumnName(versionField));

            log.debug("Generated UPDATE SQL with version: {}", sql);
            return sql;
        } else {
            // Старый вариант без версии
            List<String> setters = fields.stream()
                    .map(field -> String.format("%s = ?", getColumnName(field)))
                    .collect(Collectors.toList());

            String sql = String.format(
                    "UPDATE %s SET %s WHERE %s = ?", tableName, String.join(", ", setters), getColumnName(idField));

            log.debug("Generated UPDATE SQL without version: {}", sql);
            return sql;
        }
    }

    /**
     * Получить SQL для получения следующего значения последовательности
     */
    public String getNextSequenceValueSql() {
        String sequenceName = metaData.getSequenceName();
        String sql = String.format("SELECT nextval('%s')", sequenceName);
        log.debug("Generated NEXT SEQUENCE VALUE SQL: {}", sql);
        return sql;
    }

    private String getTableName() {
        return metaData.getName().toLowerCase();
    }

    private String getColumnName(Field field) {
        return field.getName().toLowerCase();
    }
}

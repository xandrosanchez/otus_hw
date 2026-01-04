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
        String tableName = getTableName(); // используем новый метод
        List<String> columns = metaData.getAllFields().stream()
                .map(field -> getColumnName(field)) // используем новый метод
                .collect(Collectors.toList());

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

        if (strategy.isIdentity()) {
            List<Field> fields = metaData.getFieldsWithoutId();

            List<String> columns =
                    fields.stream().map(field -> getColumnName(field)).collect(Collectors.toList());

            List<String> placeholders = fields.stream().map(field -> "?").collect(Collectors.toList());

            String sql = String.format(
                    "INSERT INTO %s (%s) VALUES (%s)",
                    tableName, String.join(", ", columns), String.join(", ", placeholders));

            log.debug("Generated INSERT (IDENTITY) SQL: {}", sql);
            return sql;
        } else {
            // Для других стратегий включаем все поля
            List<Field> allFields = metaData.getAllFields();

            List<String> columns =
                    allFields.stream().map(field -> getColumnName(field)).collect(Collectors.toList());

            List<String> placeholders = allFields.stream().map(field -> "?").collect(Collectors.toList());

            String sql = String.format(
                    "INSERT INTO %s (%s) VALUES (%s)",
                    tableName, String.join(", ", columns), String.join(", ", placeholders));

            log.debug("Generated INSERT ({} strategy) SQL: {}", strategy.getGenerationType(), sql);
            return sql;
        }
    }

    @Override
    public String getUpdateSql() {
        String tableName = getTableName();
        Field idField = metaData.getIdField();
        List<Field> fields = metaData.getFieldsWithoutId();

        List<String> setters = fields.stream()
                .map(field -> String.format("%s = ?", getColumnName(field)))
                .collect(Collectors.toList());

        String sql = String.format(
                "UPDATE %s SET %s WHERE %s = ?", tableName, String.join(", ", setters), getColumnName(idField));

        log.debug("Generated UPDATE SQL: {}", sql);
        return sql;
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

    /**
     * Получить имя таблицы в нижнем регистре
     */
    private String getTableName() {
        return metaData.getName().toLowerCase();
    }

    /**
     * Получить имя колонки в нижнем регистре
     */
    private String getColumnName(Field field) {
        return field.getName().toLowerCase();
    }
}

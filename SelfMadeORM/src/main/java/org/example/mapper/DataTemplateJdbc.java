package org.example.mapper;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.example.core.repository.DataTemplate;
import org.example.core.repository.DataTemplateException;
import org.example.core.repository.executor.DbExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Сохраняет объект в базу, читает объект из базы
 * с поддержкой разных стратегий генерации ID
 */
@SuppressWarnings("java:S1068")
public class DataTemplateJdbc<T> implements DataTemplate<T> {

    private static final Logger log = LoggerFactory.getLogger(DataTemplateJdbc.class);

    private final DbExecutor dbExecutor;
    private final EntitySQLMetaData entitySQLMetaData;
    private final EntityClassMetaData<T> entityClassMetaData;

    public DataTemplateJdbc(
            DbExecutor dbExecutor, EntitySQLMetaData entitySQLMetaData, EntityClassMetaData<T> entityClassMetaData) {
        this.dbExecutor = dbExecutor;
        this.entitySQLMetaData = entitySQLMetaData;
        this.entityClassMetaData = entityClassMetaData;
    }

    @Override
    public Optional<T> findById(Connection connection, long id) {
        return findByIdInternal(connection, id);
    }

    /**
     * Найти объект по UUID
     */
    public Optional<T> findByUuid(Connection connection, UUID uuid) {
        return findByIdInternal(connection, uuid);
    }

    private Optional<T> findByIdInternal(Connection connection, Object id) {
        String sql = entitySQLMetaData.getSelectByIdSql();
        return dbExecutor.executeSelect(connection, sql, List.of(id), rs -> {
            try {
                if (rs.next()) {
                    return createObject(rs);
                }
            } catch (SQLException e) {
                throw new DataTemplateException(e);
            }
            return null;
        });
    }

    @Override
    public List<T> findAll(Connection connection) {
        String sql = entitySQLMetaData.getSelectAllSql();

        return dbExecutor
                .executeSelect(connection, sql, List.of(), rs -> {
                    List<T> result = new ArrayList<>();
                    try {
                        while (rs.next()) {
                            result.add(createObject(rs));
                        }
                    } catch (SQLException e) {
                        throw new DataTemplateException(e);
                    }
                    return result;
                })
                .orElseThrow(() -> new DataTemplateException("Ошибка при получении всех записей"));
    }

    @Override
    public long insert(Connection connection, T entity) {
        String sql = entitySQLMetaData.getInsertSql();
        log.debug("INSERT SQL: {}", sql);

        GenerationStrategy strategy = entityClassMetaData.getGenerationStrategy();
        log.debug("Generation strategy: {}", strategy);

        // Для SEQUENCE стратегии, если ID уже установлен, используем его
        // Если не установлен - получаем из последовательности
        if (strategy.isSequence()) {
            Field idField = entityClassMetaData.getIdField();
            try {
                idField.setAccessible(true);
                Object currentId = idField.get(entity);

                if (currentId == null) {
                    // Получаем следующий ID из последовательности
                    long nextId = getNextSequenceValue(connection);
                    log.debug("Got next sequence value: {}", nextId);

                    // Устанавливаем ID в объект
                    idField.set(entity, nextId);
                } else {
                    log.debug("Using predefined ID: {}", currentId);
                }
            } catch (IllegalAccessException e) {
                throw new DataTemplateException("Ошибка доступа к полю ID", e);
            }
        }

        List<Object> params = prepareInsertParams(entity, strategy);
        log.debug("INSERT params: {}", params);

        // Для UUID стратегии просто выполняем INSERT без ожидания сгенерированного ключа
        if (strategy.isUuid()) {
            dbExecutor.executeStatement(connection, sql, params, false);
            return 0;
        }
        // Для IDENTITY стратегии возвращаем сгенерированный ID
        else if (strategy.isIdentity()) {
            return dbExecutor.executeStatement(connection, sql, params, true);
        }
        // Для SEQUENCE стратегии выполняем INSERT
        else if (strategy.isSequence()) {
            long result = dbExecutor.executeStatement(connection, sql, params, true);
            log.debug("INSERT result for SEQUENCE: {}", result);
            return result;
        }
        // Для AUTO стратегии (по умолчанию как IDENTITY)
        else {
            return dbExecutor.executeStatement(connection, sql, params, true);
        }
    }

    @Override
    public void update(Connection connection, T entity) {
        String sql = entitySQLMetaData.getUpdateSql();
        log.debug("UPDATE SQL: {}", sql);

        List<Field> fieldsWithoutId = entityClassMetaData.getFieldsWithoutId();
        List<Object> params = new ArrayList<>(fieldsWithoutId.stream()
                .map(field -> {
                    try {
                        field.setAccessible(true);
                        Object value = field.get(entity);
                        log.debug("Field {} = {}", field.getName(), value);
                        return value;
                    } catch (IllegalAccessException e) {
                        throw new DataTemplateException(e);
                    }
                })
                .toList());

        Field idField = entityClassMetaData.getIdField();
        try {
            idField.setAccessible(true);
            Object idValue = idField.get(entity);
            params.add(idValue);
            log.debug("ID field {} = {}", idField.getName(), idValue);
        } catch (IllegalAccessException e) {
            throw new DataTemplateException(e);
        }

        log.debug("UPDATE params: {}", params);

        // Для UPDATE не ожидаем сгенерированных ключей
        long affectedRows = dbExecutor.executeStatement(connection, sql, params, false);
        log.debug("UPDATE affected rows: {}", affectedRows);

        if (affectedRows == 0) {
            // Проверим, существует ли запись
            String checkSql = entitySQLMetaData.getSelectByIdSql();
            try {
                var exists = dbExecutor
                        .executeSelect(connection, checkSql, List.of(idField.get(entity)), rs -> {
                            try {
                                return rs.next();
                            } catch (SQLException e) {
                                throw new DataTemplateException(e);
                            }
                        })
                        .orElse(false);

                if (!exists) {
                    throw new DataTemplateException(
                            "Объект не найден для обновления. SQL: " + sql + ", params: " + params);
                } else {
                    log.debug("UPDATE не изменил данные (возможно, они идентичны)");
                }
            } catch (Exception e) {
                throw new DataTemplateException("Ошибка при проверке существования записи: " + e.getMessage(), e);
            }
        }
    }

    private List<Object> prepareInsertParams(T entity, GenerationStrategy strategy) {
        if (strategy.isIdentity()) {
            // Для IDENTITY включаем только поля без ID
            List<Field> fieldsWithoutId = entityClassMetaData.getFieldsWithoutId();
            return fieldsWithoutId.stream()
                    .map(field -> {
                        try {
                            field.setAccessible(true);
                            return field.get(entity);
                        } catch (IllegalAccessException e) {
                            throw new DataTemplateException(e);
                        }
                    })
                    .toList();
        } else {
            // Для других стратегий включаем все поля
            List<Field> allFields = entityClassMetaData.getAllFields();
            return allFields.stream()
                    .map(field -> {
                        try {
                            field.setAccessible(true);
                            Object value = field.get(entity);

                            // Для UUID, если значение null, генерируем новый
                            if (value == null
                                    && strategy.isUuid()
                                    && field.getType().equals(UUID.class)) {
                                value = UUID.randomUUID();
                                field.set(entity, value);
                            }

                            return value;
                        } catch (IllegalAccessException e) {
                            throw new DataTemplateException(e);
                        }
                    })
                    .toList();
        }
    }

    private long getNextSequenceValue(Connection connection) {
        if (entitySQLMetaData instanceof EntitySQLMetaDataImpl) {
            EntitySQLMetaDataImpl<T> sqlMetaData = (EntitySQLMetaDataImpl<T>) entitySQLMetaData;
            String sql = sqlMetaData.getNextSequenceValueSql();
            return dbExecutor
                    .executeSelect(connection, sql, List.of(), rs -> {
                        try {
                            if (rs.next()) {
                                return rs.getLong(1);
                            }
                        } catch (SQLException e) {
                            throw new DataTemplateException("Ошибка при получении значения последовательности", e);
                        }
                        throw new DataTemplateException("Не удалось получить значение последовательности");
                    })
                    .orElseThrow(() -> new DataTemplateException("Ошибка последовательности"));
        } else {
            throw new DataTemplateException("EntitySQLMetaData не поддерживает последовательности");
        }
    }

    private void setIdFieldValue(T entity, Object value) {
        try {
            Field idField = entityClassMetaData.getIdField();
            idField.setAccessible(true);
            idField.set(entity, value);
        } catch (IllegalAccessException e) {
            throw new DataTemplateException("Ошибка установки значения ID", e);
        }
    }

    private T createObject(ResultSet rs) throws SQLException {
        try {
            Constructor<T> constructor = entityClassMetaData.getConstructor();

            T instance = constructor.newInstance();
            List<Field> allFields = entityClassMetaData.getAllFields();
            for (Field field : allFields) {
                field.setAccessible(true);

                Class<?> fieldType = field.getType();
                String fieldName = field.getName().toLowerCase(); // lowercase для поиска в ResultSet

                Object value = getValueFromResultSet(rs, fieldName, fieldType);
                field.set(instance, value);
            }
            return instance;
        } catch (Exception e) {
            throw new SQLException("Ошибка создания объекта", e);
        }
    }

    private Object getValueFromResultSet(ResultSet rs, String fieldName, Class<?> fieldType) throws SQLException {
        try {
            if (fieldType.equals(UUID.class)) {
                String uuidStr = rs.getString(fieldName);
                return uuidStr != null ? UUID.fromString(uuidStr) : null;
            } else if (fieldType.equals(Long.class)) {
                long value = rs.getLong(fieldName);
                return rs.wasNull() ? null : value;
            } else if (fieldType.equals(Integer.class)) {
                int value = rs.getInt(fieldName);
                return rs.wasNull() ? null : value;
            } else if (fieldType.equals(Boolean.class)) {
                boolean value = rs.getBoolean(fieldName);
                return rs.wasNull() ? null : value;
            } else if (fieldType.equals(String.class)) {
                return rs.getString(fieldName);
            } else if (fieldType.equals(Double.class)) {
                double value = rs.getDouble(fieldName);
                return rs.wasNull() ? null : value;
            } else {
                throw new IllegalArgumentException("Unsupported field type: " + fieldType);
            }
        } catch (SQLException e) {
            // Пробуем альтернативные имена полей
            if (fieldName.endsWith("id")) {
                String altName = fieldName.replace("id", "Id");
                try {
                    return getValueFromResultSetAlternative(rs, altName, fieldType);
                } catch (SQLException e2) {
                    // игнорируем, пробуем дальше
                }
            }
            throw new SQLException("Ошибка чтения поля '" + fieldName + "' из ResultSet", e);
        }
    }

    private Object getValueFromResultSetAlternative(ResultSet rs, String altFieldName, Class<?> fieldType)
            throws SQLException {
        if (fieldType.equals(UUID.class)) {
            String uuidStr = rs.getString(altFieldName);
            return uuidStr != null ? UUID.fromString(uuidStr) : null;
        } else if (fieldType.equals(Long.class)) {
            long value = rs.getLong(altFieldName);
            return rs.wasNull() ? null : value;
        } else if (fieldType.equals(Integer.class)) {
            int value = rs.getInt(altFieldName);
            return rs.wasNull() ? null : value;
        } else if (fieldType.equals(Boolean.class)) {
            boolean value = rs.getBoolean(altFieldName);
            return rs.wasNull() ? null : value;
        } else if (fieldType.equals(String.class)) {
            return rs.getString(altFieldName);
        } else if (fieldType.equals(Double.class)) {
            double value = rs.getDouble(altFieldName);
            return rs.wasNull() ? null : value;
        } else {
            throw new IllegalArgumentException("Unsupported field type: " + fieldType);
        }
    }
}

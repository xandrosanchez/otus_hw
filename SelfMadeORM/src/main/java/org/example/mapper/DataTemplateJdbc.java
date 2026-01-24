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

    public Optional<T> findById(Connection connection, Object id, Class<?> idType) {
        return findByIdInternal(connection, id);
    }

    public Optional<T> findByUuid(Connection connection, UUID uuid) {
        return findByIdInternal(connection, uuid);
    }

    private Optional<T> findByIdInternal(Connection connection, Object id) {
        String sql = entitySQLMetaData.getSelectByIdSql();
        log.debug("Finding by ID: {}, SQL: {}", id, sql);

        return dbExecutor.executeSelect(connection, sql, List.of(id), rs -> {
            try {
                if (rs.next()) {
                    T entity = createObject(rs);
                    log.debug("Found entity: {}", entity);
                    return entity;
                }
                log.debug("Entity not found with ID: {}", id);
                return null;
            } catch (SQLException e) {
                throw new DataTemplateException(e);
            }
        });
    }

    @Override
    public List<T> findAll(Connection connection) {
        String sql = entitySQLMetaData.getSelectAllSql();
        log.debug("Find all SQL: {}", sql);

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
                    log.debug("Found {} entities", result.size());
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

        long generatedId = 0;

        // Для UUID стратегии просто выполняем INSERT без ожидания сгенерированного ключа
        if (strategy.isUuid()) {
            long result = dbExecutor.executeStatement(connection, sql, params, false);
            log.debug("INSERT result for UUID: {}", result);

            // UUID уже установлен в prepareInsertParams, просто читаем версию
            readVersionAfterInsert(connection, entity);
            return result;
        }
        // Для IDENTITY стратегии возвращаем сгенерированный ID
        else if (strategy.isIdentity()) {
            generatedId = dbExecutor.executeStatement(connection, sql, params, true);
            log.debug("INSERT result for IDENTITY: {}", generatedId);

            // УСТАНАВЛИВАЕМ сгенерированный ID в объект
            Field idField = entityClassMetaData.getIdField();
            try {
                idField.setAccessible(true);
                idField.set(entity, generatedId);
                log.debug("Set generated ID {} in entity", generatedId);
            } catch (IllegalAccessException e) {
                throw new DataTemplateException("Ошибка установки сгенерированного ID", e);
            }

            // После вставки читаем сгенерированную версию (если есть)
            readVersionAfterInsert(connection, entity);
            return generatedId;
        }
        // Для SEQUENCE стратегии выполняем INSERT
        else if (strategy.isSequence()) {
            long result = dbExecutor.executeStatement(connection, sql, params, true);
            log.debug("INSERT result for SEQUENCE: {}", result);

            // ID уже установлен перед INSERT, читаем версию
            readVersionAfterInsert(connection, entity);
            return result;
        }
        // Для AUTO стратегии (по умолчанию как IDENTITY)
        else {
            generatedId = dbExecutor.executeStatement(connection, sql, params, true);
            log.debug("INSERT result for AUTO: {}", generatedId);

            // Устанавливаем сгенерированный ID в объект
            Field idField = entityClassMetaData.getIdField();
            try {
                idField.setAccessible(true);
                idField.set(entity, generatedId);
                log.debug("Set generated ID {} in entity for AUTO strategy", generatedId);
            } catch (IllegalAccessException e) {
                throw new DataTemplateException("Ошибка установки сгенерированного ID", e);
            }

            readVersionAfterInsert(connection, entity);
            return generatedId;
        }
    }

    @Override
    public void update(Connection connection, T entity) {
        String sql = entitySQLMetaData.getUpdateSql();
        log.debug("UPDATE SQL: {}", sql);

        List<Object> params = prepareUpdateParams(entity);
        log.debug("UPDATE params: {}", params);

        // Для UPDATE не ожидаем сгенерированных ключей
        long affectedRows = dbExecutor.executeStatement(connection, sql, params, false);
        log.debug("UPDATE affected rows: {}", affectedRows);

        if (affectedRows == 0) {
            // Проверим, существует ли запись с такой версией
            if (!recordExistsWithVersion(connection, entity)) {
                throw new OptimisticLockException("Объект не найден для обновления или была изменена версия. SQL: "
                        + sql + ", params: " + params);
            } else {
                log.warn("UPDATE не изменил ни одной строки, хотя запись существует");
            }
        } else {
            // Увеличиваем версию в объекте после успешного UPDATE
            incrementVersionInEntity(entity);
            log.debug("Version incremented in entity");
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
                String fieldName = field.getName().toLowerCase();

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

    private List<Object> prepareInsertParams(T entity, GenerationStrategy strategy) {
        List<Field> fieldsToInsert;

        if (strategy.isIdentity()) {
            // Для IDENTITY включаем все поля без ID
            fieldsToInsert = entityClassMetaData.getFieldsWithoutId();
        } else {
            // Для других стратегий включаем все поля
            fieldsToInsert = entityClassMetaData.getAllFields();
        }

        return fieldsToInsert.stream()
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

                        // Если поле версии не установлено, устанавливаем 0
                        if (value == null && field.isAnnotationPresent(org.example.Version.class)) {
                            value = 0L;
                            field.set(entity, value);
                        }

                        return value;
                    } catch (IllegalAccessException e) {
                        throw new DataTemplateException(e);
                    }
                })
                .toList();
    }

    private List<Object> prepareUpdateParams(T entity) {
        // Получаем все поля без ID
        List<Field> fields = entityClassMetaData.getFieldsWithoutId();
        List<Object> params = new ArrayList<>();

        for (Field field : fields) {
            try {
                field.setAccessible(true);

                // Поле версии не включаем в обычные параметры SET,
                // так как оно обновляется как version = version + 1
                if (field.isAnnotationPresent(org.example.Version.class)) {
                    continue;
                }

                Object value = field.get(entity);
                params.add(value);
                log.debug("Update param field {} = {}", field.getName(), value);
            } catch (IllegalAccessException e) {
                throw new DataTemplateException(e);
            }
        }

        // Добавляем ID
        Field idField = entityClassMetaData.getIdField();
        try {
            idField.setAccessible(true);
            Object idValue = idField.get(entity);
            params.add(idValue);
            log.debug("Update param ID {} = {}", idField.getName(), idValue);
        } catch (IllegalAccessException e) {
            throw new DataTemplateException(e);
        }

        // Если есть поле версии, добавляем его в WHERE
        if (entityClassMetaData.hasVersionField()) {
            Field versionField = entityClassMetaData.getVersionField();
            try {
                versionField.setAccessible(true);
                Object versionValue = versionField.get(entity);
                params.add(versionValue);
                log.debug("Update param version {} = {}", versionField.getName(), versionValue);
            } catch (IllegalAccessException e) {
                throw new DataTemplateException(e);
            }
        }

        return params;
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

    private void readVersionAfterInsert(Connection connection, T entity) {
        if (entityClassMetaData.hasVersionField()) {
            Field idField = entityClassMetaData.getIdField();
            try {
                idField.setAccessible(true);
                Object idValue = idField.get(entity);

                // Читаем только что вставленную запись, чтобы получить актуальную версию
                Optional<T> insertedEntity = findByIdInternal(connection, idValue);
                if (insertedEntity.isPresent()) {
                    Field versionField = entityClassMetaData.getVersionField();
                    versionField.setAccessible(true);
                    Object insertedVersion = versionField.get(insertedEntity.get());

                    // Обновляем версию в исходном объекте
                    versionField.set(entity, insertedVersion);
                    log.debug("Read version after insert: {}", insertedVersion);
                }
            } catch (IllegalAccessException e) {
                throw new DataTemplateException("Ошибка чтения версии после вставки", e);
            }
        }
    }

    private boolean recordExistsWithVersion(Connection connection, T entity) {
        if (!entityClassMetaData.hasVersionField()) {
            return recordExists(connection, entity);
        }

        Field idField = entityClassMetaData.getIdField();
        Field versionField = entityClassMetaData.getVersionField();

        try {
            idField.setAccessible(true);
            versionField.setAccessible(true);

            Object idValue = idField.get(entity);
            Object versionValue = versionField.get(entity);

            String sql = entitySQLMetaData.getSelectByIdSql() + " AND " + getColumnName(versionField) + " = ?";

            return dbExecutor
                    .executeSelect(connection, sql, List.of(idValue, versionValue), rs -> {
                        try {
                            return rs.next();
                        } catch (SQLException e) {
                            throw new DataTemplateException(e);
                        }
                    })
                    .orElse(false);

        } catch (IllegalAccessException e) {
            throw new DataTemplateException(e);
        }
    }

    private boolean recordExists(Connection connection, T entity) {
        Field idField = entityClassMetaData.getIdField();
        try {
            idField.setAccessible(true);
            Object idValue = idField.get(entity);

            String sql = entitySQLMetaData.getSelectByIdSql();
            return dbExecutor
                    .executeSelect(connection, sql, List.of(idValue), rs -> {
                        try {
                            return rs.next();
                        } catch (SQLException e) {
                            throw new DataTemplateException(e);
                        }
                    })
                    .orElse(false);

        } catch (IllegalAccessException e) {
            throw new DataTemplateException(e);
        }
    }

    private void incrementVersionInEntity(T entity) {
        if (entityClassMetaData.hasVersionField()) {
            Field versionField = entityClassMetaData.getVersionField();
            try {
                versionField.setAccessible(true);
                Long currentVersion = (Long) versionField.get(entity);
                if (currentVersion != null) {
                    versionField.set(entity, currentVersion + 1);
                } else {
                    versionField.set(entity, 1L);
                }
            } catch (IllegalAccessException e) {
                throw new DataTemplateException("Ошибка инкремента версии", e);
            }
        }
    }

    private String getColumnName(Field field) {
        return field.getName().toLowerCase();
    }
}

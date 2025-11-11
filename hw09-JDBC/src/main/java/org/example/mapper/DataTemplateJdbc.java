package org.example.mapper;

import org.example.core.repository.DataTemplate;
import org.example.core.repository.DataTemplateException;
import org.example.core.repository.executor.DbExecutor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Сохратяет объект в базу, читает объект из базы
 */
@SuppressWarnings("java:S1068")
public class DataTemplateJdbc<T> implements DataTemplate<T> {

    private final DbExecutor dbExecutor;
    private final EntitySQLMetaData entitySQLMetaData;
    private final EntityClassMetaData entityClassMetaData;

    public DataTemplateJdbc(
            DbExecutor dbExecutor,
            EntitySQLMetaData entitySQLMetaData,
            EntityClassMetaData entityClassMetaData) {
        this.dbExecutor = dbExecutor;
        this.entitySQLMetaData = entitySQLMetaData;
        this.entityClassMetaData = entityClassMetaData;
    }

    @Override
    public Optional<T> findById(Connection connection, long id) {
        String sql = entitySQLMetaData.getSelectByIdSql();
        return dbExecutor.executeSelect(
                connection,
                sql,
                List.of(id),
                rs -> {
                    try {
                        if (rs.next()) {
                            return createObject(rs);
                        }
                    } catch (SQLException e) {
                        throw new DataTemplateException(e);
                    }
                    return null;
                }
        );
    }

    @Override
    public List<T> findAll(Connection connection) {
        String sql = entitySQLMetaData.getSelectAllSql();

        return dbExecutor.executeSelect(
                connection,
                sql,
                List.of(),
                rs -> {
                    List<T> result = new ArrayList<>();
                    try {
                        while (rs.next()) {
                            result.add(createObject(rs));
                        }
                    } catch (SQLException e) {
                        throw new DataTemplateException(e);
                    }
                    return result;
                }
        ).orElseThrow(() -> new DataTemplateException("Ошибка при получении всех записей"));
    }


    @Override
    public long insert(Connection connection, T client) {
        String sql = entitySQLMetaData.getInsertSql();

        List<Field> fieldsWithoutId = entityClassMetaData.getFieldsWithoutId();
        List<Object> params = fieldsWithoutId.stream()
                .map(field -> {
                    try {
                        field.setAccessible(true);
                        return field.get(client);
                    } catch (IllegalAccessException e) {
                        throw new DataTemplateException(e);
                    }
                })
                .toList();

        return dbExecutor.executeStatement(connection, sql, params);
    }

    @Override
    public void update(Connection connection, T client) {
        String sql = entitySQLMetaData.getUpdateSql();

        List<Field> fieldsWithoutId = entityClassMetaData.getFieldsWithoutId();
        List<Object> params = new ArrayList<>(fieldsWithoutId.stream()
                .map(field -> {
                    try {
                        field.setAccessible(true);
                        return field.get(client);
                    } catch (IllegalAccessException e) {
                        throw new DataTemplateException(e);
                    }
                })
                .toList());

        Field idField = entityClassMetaData.getIdField();
        try {
            idField.setAccessible(true);
            params.add(idField.getLong(client));
        } catch (IllegalAccessException e) {
            throw new DataTemplateException(e);
        }

        int affectedRows = (int) dbExecutor.executeStatement(connection, sql, params);
        if (affectedRows == 0) {
            throw new DataTemplateException("Объект не найден для обновления");
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

                Object value = switch (fieldType.getSimpleName()) {
                    case "Long" -> rs.getLong(fieldName);
                    case "String" -> rs.getString(fieldName);
                    case "Integer" -> rs.getInt(fieldName);
                    case "Boolean" -> rs.getBoolean(fieldName);
                    default -> throw new IllegalArgumentException("Unsupported field type: " + fieldType);
                };

                field.set(instance, value);
            }
            return instance;
        } catch (Exception e) {
            throw new SQLException("Ошибка создания объекта", e);
        }
    }
}

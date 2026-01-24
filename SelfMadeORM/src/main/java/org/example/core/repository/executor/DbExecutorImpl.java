package org.example.core.repository.executor;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.example.core.sessionmanager.DataBaseOperationException;

public class DbExecutorImpl implements DbExecutor {

    @Override
    public long executeStatement(Connection connection, String sql, List<Object> params) {
        return executeStatement(connection, sql, params, true);
    }

    @Override
    public long executeStatement(Connection connection, String sql, List<Object> params, boolean returnGeneratedKeys) {
        try (var pst = returnGeneratedKeys
                ? connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
                : connection.prepareStatement(sql)) {

            for (var idx = 0; idx < params.size(); idx++) {
                pst.setObject(idx + 1, params.get(idx));
            }

            int affectedRows = pst.executeUpdate(); // ← Сохраняем количество измененных строк

            if (returnGeneratedKeys) {
                try (var rs = pst.getGeneratedKeys()) {
                    if (rs.next()) {
                        Object key = rs.getObject(1);
                        if (key instanceof Number number) {
                            return number.longValue();
                        } else if (key != null) {
                            // Для UUID и других нечисловых ключей возвращаем affectedRows
                            return affectedRows;
                        }
                    }
                }
                return affectedRows; // Если нет ключей, но запрос выполнился
            }
            return affectedRows; // ← Возвращаем количество измененных строк для UPDATE/DELETE
        } catch (SQLException ex) {
            throw new DataBaseOperationException("executeStatement error. SQL: " + sql + ", params: " + params, ex);
        }
    }

    @Override
    public <T> Optional<T> executeSelect(
            Connection connection, String sql, List<Object> params, Function<ResultSet, T> rsHandler) {
        try (var pst = connection.prepareStatement(sql)) {
            for (var idx = 0; idx < params.size(); idx++) {
                pst.setObject(idx + 1, params.get(idx));
            }
            try (var rs = pst.executeQuery()) {
                return Optional.ofNullable(rsHandler.apply(rs));
            }
        } catch (SQLException ex) {
            throw new DataBaseOperationException("executeSelect error. SQL: " + sql + ", params: " + params, ex);
        }
    }
}

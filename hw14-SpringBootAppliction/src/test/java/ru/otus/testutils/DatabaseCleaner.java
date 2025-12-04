package ru.otus.testutils;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseCleaner {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseCleaner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void clean() {
        // Отключаем foreign key проверки для надежной очистки
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");

        jdbcTemplate.execute("DELETE FROM PHONE");
        jdbcTemplate.execute("DELETE FROM ADDRESS");
        jdbcTemplate.execute("DELETE FROM CLIENT");

        // Сброс последовательностей
        jdbcTemplate.execute("ALTER TABLE CLIENT ALTER COLUMN ID RESTART WITH 1");
        jdbcTemplate.execute("ALTER TABLE ADDRESS ALTER COLUMN ID RESTART WITH 1");
        jdbcTemplate.execute("ALTER TABLE PHONE ALTER COLUMN ID RESTART WITH 1");

        // Включаем обратно
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
    }
}

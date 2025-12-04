package ru.otus;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class SimpleDatabaseTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldConnectToDatabase() {
        // Простейший тест - проверяем соединение с БД
        Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        assertThat(result).isEqualTo(1);
    }

    @Test
    void shouldCreateSimpleTable() {
        // Создаем простую таблицу для проверки
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS TEST_TABLE (ID INT PRIMARY KEY, NAME VARCHAR(255))");
        jdbcTemplate.execute("INSERT INTO TEST_TABLE VALUES (1, 'test')");

        String name = jdbcTemplate.queryForObject("SELECT NAME FROM TEST_TABLE WHERE ID = 1", String.class);
        assertThat(name).isEqualTo("test");

        jdbcTemplate.execute("DROP TABLE TEST_TABLE");
    }
}

import javax.sql.DataSource;
import org.example.crm.datasource.DriverManagerDataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public abstract class BaseOrmTest {

    @Container
    protected static final PostgreSQLContainer<?> POSTGRESQL_CONTAINER = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");

    protected DataSource dataSource;

    @BeforeEach
    void setUp() {
        String url = POSTGRESQL_CONTAINER.getJdbcUrl();
        String user = POSTGRESQL_CONTAINER.getUsername();
        String password = POSTGRESQL_CONTAINER.getPassword();

        dataSource = new DriverManagerDataSource(url, user, password);
        runMigrations(dataSource);
    }

    @AfterEach
    void tearDown() {
        // Testcontainers автоматически очистит контейнер
    }

    private void runMigrations(DataSource dataSource) {
        var flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:/migration")
                .load();
        flyway.migrate();
    }
}

package ru.otus;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.otus.core.repository.DataTemplateHibernate;
import ru.otus.core.repository.HibernateUtils;
import ru.otus.core.sessionmanager.TransactionManagerHibernate;
import ru.otus.crm.dbmigrations.MigrationsExecutorFlyway;
import ru.otus.crm.model.Address;
import ru.otus.crm.model.Client;
import ru.otus.crm.model.Phone;
import ru.otus.crm.service.DBServiceClient;
import ru.otus.crm.service.DbServiceClientImpl;
import ru.otus.server.CrmWebServer;
import ru.otus.server.CrmWebServerWithFilterBasedSecurity;
import ru.otus.services.TemplateProcessor;
import ru.otus.services.TemplateProcessorImpl;
import ru.otus.services.UserAuthService;
import ru.otus.services.UserAuthServiceImpl;

@SuppressWarnings("java:S2925")
class CrmWebServerIntegrationTest {
    private static final int WEB_SERVER_PORT = 8989;
    private static final String WEB_SERVER_URL = "http://localhost:" + WEB_SERVER_PORT;

    private static CrmWebServer webServer;
    private static HttpClient http;
    private static DBServiceClient dbServiceClient;

    @BeforeAll
    static void setUp() throws Exception {
        Configuration configuration = new Configuration();

        String dbUrl = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1";
        String dbUserName = "sa";
        String dbPassword = "";

        configuration.setProperty("hibernate.connection.url", dbUrl);
        configuration.setProperty("hibernate.connection.username", dbUserName);
        configuration.setProperty("hibernate.connection.password", dbPassword);
        configuration.setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        configuration.setProperty("hibernate.hbm2ddl.auto", "create-drop");
        configuration.setProperty("hibernate.show_sql", "false");

        // Выполняем миграции
        new MigrationsExecutorFlyway(dbUrl, dbUserName, dbPassword).executeMigrations();

        var sessionFactory =
                HibernateUtils.buildSessionFactory(configuration, Client.class, Address.class, Phone.class);
        var transactionManager = new TransactionManagerHibernate(sessionFactory);
        var clientTemplate = new DataTemplateHibernate<>(Client.class);
        dbServiceClient = new DbServiceClientImpl(transactionManager, clientTemplate);

        // Настройка веб-сервера
        Gson gson = new GsonBuilder().serializeNulls().setPrettyPrinting().create();
        TemplateProcessor templateProcessor = new TemplateProcessorImpl("/templates/");
        UserAuthService authService = new UserAuthServiceImpl();

        webServer = new CrmWebServerWithFilterBasedSecurity(
                WEB_SERVER_PORT, authService, dbServiceClient, gson, templateProcessor);

        http = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER) // Не следовать редиректам автоматически
                .build();
        webServer.start();

        // Даем серверу время для запуска
        Thread.sleep(1000);
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (webServer != null) {
            webServer.stop();
        }
    }

    @Test
    void shouldCreateClientViaApi() throws Exception {
        // given
        String clientJson = "{\"name\":\"API Test Client\"}";

        // when
        HttpRequest request = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(clientJson))
                .uri(URI.create(WEB_SERVER_URL + "/api/client"))
                .header("Content-Type", "application/json")
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

        // then
        assertThat(response.statusCode()).isEqualTo(HttpURLConnection.HTTP_CREATED);
        assertThat(response.body()).contains("API Test Client");

        // Проверяем, что клиент действительно создан
        List<Client> clients = dbServiceClient.findAll();
        assertThat(clients).anyMatch(client -> "API Test Client".equals(client.getName()));
    }

    @Test
    void shouldGetClientByIdViaApi() throws Exception {
        // given
        Client client = new Client("Test Client for API");
        Client savedClient = dbServiceClient.saveClient(client);
        long clientId = savedClient.getId();

        // when
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(WEB_SERVER_URL + "/api/client/" + clientId))
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

        // then
        assertThat(response.statusCode()).isEqualTo(HttpURLConnection.HTTP_OK);
        assertThat(response.body()).contains("Test Client for API");
    }
}

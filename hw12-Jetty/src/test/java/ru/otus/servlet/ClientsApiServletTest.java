package ru.otus.servlet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.otus.crm.model.Client;
import ru.otus.crm.service.DBServiceClient;

@SuppressWarnings({"java:S5853", "java:S6126"})
@ExtendWith(MockitoExtension.class)
class ClientsApiServletTest {

    @Mock
    private DBServiceClient dbServiceClient;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private ClientsApiServlet servlet;
    private Gson gson;
    private StringWriter responseWriter;

    @BeforeEach
    void setUp() throws IOException {
        gson = new GsonBuilder().serializeNulls().setPrettyPrinting().create();
        servlet = new ClientsApiServlet(dbServiceClient, gson);
        responseWriter = new StringWriter();
        lenient().when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
    }

    @Test
    void shouldReturnAllClients() throws IOException {
        // given
        List<Client> clients = List.of(new Client(1L, "Client 1"), new Client(2L, "Client 2"));
        when(dbServiceClient.findAll()).thenReturn(clients);
        when(request.getPathInfo()).thenReturn("/");

        // when
        servlet.doGet(request, response);

        // then
        verify(response).setContentType("application/json;charset=UTF-8");
        String jsonResponse = responseWriter.toString();
        assertThat(jsonResponse)
                .isEqualToIgnoringCase("[\n" + "  {\n"
                        + "    \"id\": 1,\n"
                        + "    \"name\": \"Client 1\",\n"
                        + "    \"address\": null,\n"
                        + "    \"phones\": []\n"
                        + "  },\n"
                        + "  {\n"
                        + "    \"id\": 2,\n"
                        + "    \"name\": \"Client 2\",\n"
                        + "    \"address\": null,\n"
                        + "    \"phones\": []\n"
                        + "  }\n"
                        + "]");
    }

    @Test
    void shouldReturnClientById() throws IOException {
        // given
        Client client = new Client(1L, "Test Client");
        when(dbServiceClient.getClient(1L)).thenReturn(Optional.of(client));
        when(request.getPathInfo()).thenReturn("/1");

        // when
        servlet.doGet(request, response);

        // then
        verify(response).setContentType("application/json;charset=UTF-8");
        String jsonResponse = responseWriter.toString();
        assertThat(jsonResponse)
                .isEqualToIgnoringCase(
                        """
                    {
                      "id": 1,
                      "name": "Test Client",
                      "address": null,
                      "phones": []
                    }""");
    }

    @Test
    void shouldReturn404WhenClientNotFound() throws IOException {
        // given
        when(dbServiceClient.getClient(1L)).thenReturn(Optional.empty());
        when(request.getPathInfo()).thenReturn("/1");

        // when
        servlet.doGet(request, response);

        // then
        verify(response).setStatus(HttpServletResponse.SC_NOT_FOUND);
    }

    @Test
    void shouldCreateNewClient() throws IOException {
        // given
        Client newClient = new Client("New Client");
        Client savedClient = new Client(1L, "New Client");
        when(dbServiceClient.saveClient(any(Client.class))).thenReturn(savedClient);

        String jsonRequest = gson.toJson(newClient);
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader(jsonRequest)));

        // when
        servlet.doPost(request, response);
        // then
        verify(response).setStatus(HttpServletResponse.SC_CREATED);
        verify(response).setContentType("application/json;charset=UTF-8");
        String jsonResponse = responseWriter.toString();
        assertThat(jsonResponse)
                .isEqualToIgnoringCase(
                        """
                    {
                      "id": 1,
                      "name": "New Client",
                      "address": null,
                      "phones": []
                    }""");
    }
}

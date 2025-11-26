package ru.otus.servlet;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.otus.crm.model.Client;
import ru.otus.crm.service.DBServiceClient;
import ru.otus.services.TemplateProcessor;

@ExtendWith(MockitoExtension.class)
class ClientsServletTest {

    @Mock
    private TemplateProcessor templateProcessor;

    @Mock
    private DBServiceClient dbServiceClient;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private ClientsServlet servlet;
    private StringWriter responseWriter;

    @BeforeEach
    void setUp() throws IOException {
        servlet = new ClientsServlet(templateProcessor, dbServiceClient);
        responseWriter = new StringWriter();
        lenient().when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
    }

    @Test
    void shouldDisplayClientsList() throws IOException {
        // given
        List<Client> clients = List.of(new Client(1L, "Client 1"), new Client(2L, "Client 2"));
        when(dbServiceClient.findAll()).thenReturn(clients);
        when(templateProcessor.getPage(eq("clients.html"), any())).thenReturn("<html>Clients List</html>");

        // when
        servlet.doGet(request, response);

        // then
        verify(response).setContentType("text/html");
        verify(templateProcessor).getPage("clients.html", java.util.Map.of("clients", clients));
    }

    @Test
    void shouldCreateNewClient() throws IOException {
        // given
        when(request.getParameter("name")).thenReturn("New Client");

        // when
        servlet.doPost(request, response);

        // then
        verify(dbServiceClient).saveClient(any(Client.class));
        verify(response).sendRedirect("/clients");
    }

    @Test
    void shouldNotCreateClientWithEmptyName() throws IOException {
        // given
        when(request.getParameter("name")).thenReturn("   ");

        // when
        servlet.doPost(request, response);

        // then
        verify(response).sendRedirect("/clients");
    }
}

package ru.otus.servlet;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.otus.services.TemplateProcessor;
import ru.otus.services.UserAuthService;

@ExtendWith(MockitoExtension.class)
class LoginServletTest {

    @Mock
    private TemplateProcessor templateProcessor;

    @Mock
    private UserAuthService userAuthService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private HttpSession session;

    private LoginServlet servlet;
    private StringWriter responseWriter;

    @BeforeEach
    void setUp() throws IOException {
        servlet = new LoginServlet(templateProcessor, userAuthService);
        responseWriter = new StringWriter();
        lenient().when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
    }

    @Test
    void shouldDisplayLoginPage() throws IOException {
        // given
        when(templateProcessor.getPage(eq("login.html"), any())).thenReturn("<html>Login</html>");

        // when
        servlet.doGet(request, response);

        // then
        verify(response).setContentType("text/html");
        verify(templateProcessor).getPage("login.html", java.util.Map.of());
    }

    @Test
    void shouldAuthenticateAndRedirectToClients() throws IOException {
        // given
        when(request.getParameter("login")).thenReturn("admin");
        when(request.getParameter("password")).thenReturn("admin");
        when(userAuthService.authenticate("admin", "admin")).thenReturn(true);
        when(request.getSession()).thenReturn(session);

        // when
        servlet.doPost(request, response);

        // then
        verify(session).setMaxInactiveInterval(30);
        verify(session).setAttribute("user", "admin");
        verify(response).sendRedirect("/clients"); // Исправлено на /clients
    }

    @Test
    void shouldNotAuthenticateWithWrongCredentials() throws IOException {
        // given
        when(request.getParameter("login")).thenReturn("admin");
        when(request.getParameter("password")).thenReturn("wrong");
        when(userAuthService.authenticate("admin", "wrong")).thenReturn(false);

        // when
        servlet.doPost(request, response);

        // then
        verify(response).sendRedirect("/login?error=true"); // Исправлено
        verify(session, never()).setMaxInactiveInterval(30);
        verify(session, never()).setAttribute(eq("user"), any());
    }
}

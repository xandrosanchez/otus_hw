package ru.otus.servlet;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthorizationFilterTest {

    @Mock
    private FilterConfig filterConfig;

    @Mock
    private ServletContext servletContext;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private HttpSession session;

    private AuthorizationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new AuthorizationFilter();
        lenient().when(filterConfig.getServletContext()).thenReturn(servletContext);
        filter.init(filterConfig);
    }

    @Test
    void shouldAllowPublicPathsWithoutAuthentication() throws Exception {
        // given
        when(request.getRequestURI()).thenReturn("/login");

        // when
        filter.doFilter(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
        verify(response, never()).sendRedirect("/login");
    }

    @Test
    void shouldAllowAccessWhenAuthenticated() throws Exception {
        // given
        when(request.getRequestURI()).thenReturn("/clients");

        // when
        filter.doFilter(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
        verify(response, never()).sendRedirect("/login");
    }
}

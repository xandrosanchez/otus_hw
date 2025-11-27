package ru.otus.services;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UserAuthServiceImplTest {

    private final UserAuthService authService = new UserAuthServiceImpl();

    @Test
    void shouldAuthenticateWithCorrectCredentials() {
        // when & then
        assertThat(authService.authenticate("admin", "admin")).isTrue();
    }

    @Test
    void shouldNotAuthenticateWithWrongCredentials() {
        // when & then
        assertThat(authService.authenticate("admin", "wrong")).isFalse();
        assertThat(authService.authenticate("user", "admin")).isFalse();
        assertThat(authService.authenticate("", "")).isFalse();
    }
}

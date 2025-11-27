package ru.otus.services;

@SuppressWarnings("java:S2068")
public class UserAuthServiceImpl implements UserAuthService {

    @Override
    public boolean authenticate(String login, String password) {
        // Простая аутентификация - только админ
        return "admin".equals(login) && "admin".equals(password);
    }
}

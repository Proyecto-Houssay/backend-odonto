package com.proyectohoussay.odonto.auth;

import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final String REGISTERED_USERNAME = "admin";
    private static final String REGISTERED_PASSWORD = "123456";

    public boolean authenticate(String usernameOrEmail, String password) {

        if (usernameOrEmail == null || usernameOrEmail.isBlank()) {
            return false;
        }

        if (password == null || password.isBlank()) {
            return false;
        }

        boolean userExists = usernameOrEmail.equals(REGISTERED_USERNAME);

        if (!userExists) {
            return false;
        }

        return password.equals(REGISTERED_PASSWORD);
    }
}
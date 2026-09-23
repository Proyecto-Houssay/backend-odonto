package com.proyectohoussay.odonto.auth;

import org.springframework.stereotype.Service;

@Service
public class AuthService {

    public boolean authenticate(String usernameOrEmail, String password) {
        return usernameOrEmail.equals("admin")
        && password.equals("123456");
    }
}

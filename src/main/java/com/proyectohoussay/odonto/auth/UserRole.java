package com.proyectohoussay.odonto.auth;

import java.util.Locale;
import java.util.Optional;

public enum UserRole {
    ADMINISTRADOR,
    ODONTOLOGO,
    RECEPCIONISTA;

    public static Optional<UserRole> from(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(UserRole.valueOf(value.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}

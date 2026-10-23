package com.proyectohoussay.odonto.auth;

public class InvalidPasswordResetTokenException extends RuntimeException {
    public InvalidPasswordResetTokenException() {
        super("El enlace de recuperación no es válido o venció. Solicitá uno nuevo.");
    }
}

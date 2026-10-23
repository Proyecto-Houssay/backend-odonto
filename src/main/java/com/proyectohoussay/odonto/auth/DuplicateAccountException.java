package com.proyectohoussay.odonto.auth;

public class DuplicateAccountException extends RuntimeException {
    public DuplicateAccountException() {
        super("El nombre de usuario o el correo electrónico ya está registrado.");
    }
}

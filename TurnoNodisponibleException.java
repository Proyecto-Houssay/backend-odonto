package com.proyectohoussay.backendodonto;

public class TurnoNoDisponibleException extends RuntimeException {
    public TurnoNoDisponibleException(String message) {
        super(message);
    }
}
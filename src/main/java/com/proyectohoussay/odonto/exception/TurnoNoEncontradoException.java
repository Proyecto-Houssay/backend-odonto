package com.proyectohoussay.odonto.exception;

public class TurnoNoEncontradoException extends RuntimeException {

    public TurnoNoEncontradoException(Long id) {
        super("Turno no encontrado con ID: " + id);
    }
}

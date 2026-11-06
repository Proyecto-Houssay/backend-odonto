package com.proyectohoussay.odonto.teletriaje.exception;

public class CitaTeletriajeNoEncontradaException extends RuntimeException {
    public CitaTeletriajeNoEncontradaException(Long id) {
        super("Cita de teletriaje no encontrada con ID: " + id);
    }
}

package com.proyectohoussay.odonto.teletriaje.exception;

public class SolicitudTeletriajeNoEncontradaException extends RuntimeException {
    public SolicitudTeletriajeNoEncontradaException(Long id) {
        super("Solicitud de teletriaje no encontrada con ID: " + id);
    }
}

package com.proyectohoussay.odonto.patient;

public class PacienteNoEncontradoException extends RuntimeException {
    public PacienteNoEncontradoException(Long id) {
        super("Paciente no encontrado con ID: " + id);
    }
}

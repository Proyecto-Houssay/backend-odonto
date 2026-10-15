package com.proyectohoussay.odonto.patient;

import java.time.LocalDate;

public record PacienteResponse(Long id, String nombre, String apellido, String dni,
                               String telefono, String email, LocalDate fechaNacimiento) {
    public static PacienteResponse de(Paciente paciente) {
        return new PacienteResponse(paciente.getId(), paciente.getNombre(), paciente.getApellido(),
                paciente.getDni(), paciente.getTelefono(), paciente.getEmail(), paciente.getFechaNacimiento());
    }
}

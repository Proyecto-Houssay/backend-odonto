package com.proyectohoussay.backendodonto;

import java.time.LocalDate;
import java.time.LocalTime;

public record TurnoResponse(
        Long id,
        Long pacienteId,
        String pacienteNombre,
        Long odontologoId,
        String odontologoNombre,
        LocalDate fecha,
        LocalTime horario,
        String motivo,
        String estado,
        String mensaje
) {
}
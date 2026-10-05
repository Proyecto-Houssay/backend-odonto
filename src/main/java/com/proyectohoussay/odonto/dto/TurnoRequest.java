package com.proyectohoussay.odonto.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

public record TurnoRequest(
        @NotNull(message = "La fecha del turno es obligatoria.") LocalDate fecha,
        @NotNull(message = "El horario del turno es obligatorio.") LocalTime hora,
        String motivo,
        String estado,
        @NotNull(message = "El paciente es obligatorio.") Long pacienteId,
        @NotNull(message = "El odontólogo es obligatorio.") Long odontologoId) {
}

package com.proyectohoussay.backendodonto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.time.LocalTime;

public record TurnoRequest(

        @NotNull(message = "El paciente es obligatorio")
        Long pacienteId,

        @NotNull(message = "El odontólogo es obligatorio")
        Long odontologoId,

        @NotNull(message = "La fecha es obligatoria")
        @FutureOrPresent(message = "La fecha no puede ser anterior a la actual")
        LocalDate fecha,

        @NotNull(message = "El horario es obligatorio")
        LocalTime horario,

        @Size(max = 500, message = "El motivo no puede superar los 500 caracteres")
        String motivo
) {
}
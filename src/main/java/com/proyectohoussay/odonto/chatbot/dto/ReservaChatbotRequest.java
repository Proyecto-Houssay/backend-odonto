package com.proyectohoussay.odonto.chatbot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

public record ReservaChatbotRequest(
        @NotNull(message = "El odontólogo es obligatorio.")
        @Positive(message = "El odontólogo indicado no es válido.")
        Long odontologoId,

        @NotNull(message = "La fecha es obligatoria.")
        LocalDate fecha,

        @NotNull(message = "El horario es obligatorio.")
        LocalTime hora,

        @NotBlank(message = "El motivo de la consulta es obligatorio.")
        @Size(max = 200, message = "El motivo no puede superar los 200 caracteres.")
        String motivo,

        Long pacienteId
) {
}

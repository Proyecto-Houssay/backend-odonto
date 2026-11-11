package com.proyectohoussay.odonto.chatbot.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record TurnoSlotDto(
        Long odontologoId,
        String odontologoNombre,
        String especialidad,
        LocalDate fecha,
        LocalTime hora,
        String etiquetaFecha,
        String etiquetaHora
) {
}

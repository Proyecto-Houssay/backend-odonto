package com.proyectohoussay.odonto.chatbot.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record ReservaChatbotResponse(
        Long turnoId,
        LocalDate fecha,
        LocalTime hora,
        String motivo,
        String estado,
        Long odontologoId,
        String odontologoNombre,
        Long pacienteId,
        String pacienteNombre,
        String mensaje
) {
}

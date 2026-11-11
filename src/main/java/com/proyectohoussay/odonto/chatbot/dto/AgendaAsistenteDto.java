package com.proyectohoussay.odonto.chatbot.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AgendaAsistenteDto(
        boolean requiereFichaPaciente,
        boolean esStaff,
        List<DoctorDisponibleDto> doctores,
        List<TurnoSlotDto> slotsDisponibles,
        LocalDate fechaConsultada,
        Long odontologoConsultadoId,
        String horaSugerida,
        String motivoSugerido,
        ReservaChatbotResponse turnoConfirmado
) {
}

package com.proyectohoussay.odonto.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record DisponibilidadAgendaResponse(
        Long odontologoId,
        LocalDate fecha,
        List<LocalTime> horariosDisponibles
) {}

package com.proyectohoussay.odonto.teletriaje.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SolicitudTeletriajeRequest(
        @NotNull(message = "El ID del paciente es obligatorio")
        Long pacienteId,

        @NotBlank(message = "El motivo de consulta es obligatorio")
        String motivoConsulta,

        String descripcionSintomas,

        String prioridadSugerida,

        Boolean signosAlarma,

        String origen
) {}

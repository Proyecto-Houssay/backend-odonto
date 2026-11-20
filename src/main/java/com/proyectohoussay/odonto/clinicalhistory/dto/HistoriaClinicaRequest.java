package com.proyectohoussay.odonto.clinicalhistory.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record HistoriaClinicaRequest(
        @NotNull(message = "El paciente es obligatorio.")
        Long pacienteId,

        LocalDate fechaApertura,

        String observaciones
) {
}

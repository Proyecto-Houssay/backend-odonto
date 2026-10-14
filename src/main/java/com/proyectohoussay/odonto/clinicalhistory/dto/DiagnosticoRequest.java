package com.proyectohoussay.odonto.clinicalhistory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record DiagnosticoRequest(
        @NotNull(message = "La historia clínica es obligatoria.")
        Long historiaClinicaId,

        LocalDate fecha,

        @NotBlank(message = "La descripción del diagnóstico es obligatoria.")
        String descripcion,

        List<TratamientoDto> tratamientos
) {
}

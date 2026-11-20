package com.proyectohoussay.odonto.clinicalhistory.dto;

import com.proyectohoussay.odonto.clinicalhistory.Diagnostico;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record DiagnosticoDto(
        Long id,
        LocalDate fecha,
        @NotBlank(message = "La descripción del diagnóstico es obligatoria.")
        String descripcion,
        Long historiaClinicaId,
        List<TratamientoDto> tratamientos
) {
    public static DiagnosticoDto de(Diagnostico d) {
        if (d == null) return null;
        List<TratamientoDto> tratamientos = d.getTratamientos() != null
                ? d.getTratamientos().stream().map(TratamientoDto::de).toList()
                : List.of();
        Long historiaId = d.getHistoriaClinica() != null ? d.getHistoriaClinica().getId() : null;
        return new DiagnosticoDto(d.getId(), d.getFecha(), d.getDescripcion(), historiaId, tratamientos);
    }
}

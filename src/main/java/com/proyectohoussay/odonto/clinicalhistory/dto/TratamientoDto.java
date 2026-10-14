package com.proyectohoussay.odonto.clinicalhistory.dto;

import com.proyectohoussay.odonto.clinicalhistory.EstadoTratamiento;
import com.proyectohoussay.odonto.clinicalhistory.Tratamiento;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record TratamientoDto(
        Long id,
        @NotBlank(message = "La descripción del tratamiento es obligatoria.")
        String descripcion,
        LocalDate fecha,
        EstadoTratamiento estado
) {
    public static TratamientoDto de(Tratamiento t) {
        if (t == null) return null;
        return new TratamientoDto(t.getId(), t.getDescripcion(), t.getFecha(), t.getEstado());
    }
}

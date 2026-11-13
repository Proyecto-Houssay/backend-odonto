package com.proyectohoussay.odonto.clinicalhistory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record TratamientoRequest(
        @NotNull(message = "El diagnóstico es obligatorio") Long diagnosticoId,
        @NotBlank(message = "La descripción del tratamiento es obligatoria") @Size(max = 500) String descripcion,
        @NotNull(message = "La fecha del tratamiento es obligatoria") LocalDate fecha,
        @NotNull(message = "El estado del tratamiento es obligatorio") EstadoTratamiento estado
) {
}

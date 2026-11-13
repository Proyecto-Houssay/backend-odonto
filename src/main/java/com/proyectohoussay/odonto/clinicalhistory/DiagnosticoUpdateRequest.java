package com.proyectohoussay.odonto.clinicalhistory;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record DiagnosticoUpdateRequest(
        @NotNull(message = "La fecha del diagnóstico es obligatoria") LocalDate fecha,
        @NotBlank(message = "La descripción del diagnóstico es obligatoria") @Size(max = 500) String descripcion
) {
}

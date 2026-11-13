package com.proyectohoussay.odonto.clinicalhistory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record DiagnosticoRequest(
        @NotNull(message = "La historia clínica es obligatoria") Long historiaClinicaId,
        @NotNull(message = "La fecha del diagnóstico es obligatoria") LocalDate fecha,
        @NotBlank(message = "La descripción del diagnóstico es obligatoria") @Size(max = 500) String descripcion
) {
}

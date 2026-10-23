package com.proyectohoussay.odonto.clinicalhistory;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record HistoriaUpdateRequest(
        @NotNull(message = "La fecha de apertura es obligatoria") LocalDate fechaApertura,
        @Size(max = 1000) String observaciones
) {
}

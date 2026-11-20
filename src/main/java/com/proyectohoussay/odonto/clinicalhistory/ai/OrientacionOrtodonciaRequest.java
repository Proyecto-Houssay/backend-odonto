package com.proyectohoussay.odonto.clinicalhistory.ai;

import jakarta.validation.constraints.NotNull;

/** Cobertura observada y cargada por el profesional que revisó el estudio. */
public record OrientacionOrtodonciaRequest(
        @NotNull(message = "Indique qué arcada requiere evaluación.")
        CoberturaOrtodoncia coberturaObservada
) {
    public enum CoberturaOrtodoncia {
        SUPERIOR,
        INFERIOR,
        AMBAS,
        INDETERMINADA
    }
}

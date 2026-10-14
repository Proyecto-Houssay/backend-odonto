package com.proyectohoussay.odonto.clinicalhistory.ai;

import jakarta.validation.constraints.NotNull;

/**
 * Request payload for analyzing a dental image with AI.
 */
public record AnalisisImagenRequest(
        @NotNull(message = "El tipo de imagen dental es obligatorio.")
        TipoImagenDental tipoImagen,

        String imagenBase64,

        Long pacienteId,

        Long historiaClinicaId,

        String observacionesOdontologo,

        boolean guardarEnHistoria,

        String nombreArchivo
) {
}

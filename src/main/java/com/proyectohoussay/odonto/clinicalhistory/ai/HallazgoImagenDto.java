package com.proyectohoussay.odonto.clinicalhistory.ai;

/**
 * Individual dental finding identified during image diagnosis.
 */
public record HallazgoImagenDto(
        PatologiaDental patologia,
        String piezaDental,
        SeveridadHallazgo severidad,
        double confianza,
        String zonaAfectada,
        String descripcion,
        String tratamientoSugerido
) {
}

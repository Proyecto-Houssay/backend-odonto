package com.proyectohoussay.odonto.clinicalhistory.ai;

/**
 * Modalities of dental images supported by the AI diagnostic system.
 */
public enum TipoImagenDental {
    RADIOGRAFIA_PANORAMICA("Radiografía Panorámica (Ortopantomografía)",
            "Evaluación global de maxilares, senos maxilares, articulación temporomandibular y piezas retenidas."),
    RADIOGRAFIA_PERIAPICAL("Radiografía Periapical Intraoral",
            "Evaluación de la anatomía completa de la pieza dental (corona, raíz, ápice, ligamento y hueso circundante)."),
    BITE_WING("Radiografía de Aleta de Mordida (Bite-wing)",
            "Detección precoz de caries interproximales y valoración de la cresta ósea alveolar en molares y premolares."),
    FOTO_INTRAORAL("Fotografía Clínica Intraoral",
            "Inspección visual de tejidos blandos, encías, tinciones, placa bacteriana y restauraciones visibles."),
    TOMOGRAFIA_CONE_BEAM("Tomografía Computarizada Cone Beam (CBCT 3D)",
            "Reconstrucción tridimensional de alta resolución para cirugía, implantes y endodoncia compleja.");

    private final String nombreLegible;
    private final String descripcion;

    TipoImagenDental(String nombreLegible, String descripcion) {
        this.nombreLegible = nombreLegible;
        this.descripcion = descripcion;
    }

    public String getNombreLegible() {
        return nombreLegible;
    }

    public String getDescripcion() {
        return descripcion;
    }
}

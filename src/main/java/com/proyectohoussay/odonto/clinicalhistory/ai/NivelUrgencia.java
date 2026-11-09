package com.proyectohoussay.odonto.clinicalhistory.ai;

/**
 * Urgency level for dental treatment prioritized by the AI diagnostic module.
 */
public enum NivelUrgencia {
    BAJA("Baja", "Control preventivo rutinario o mantenimiento higiénico."),
    MEDIA("Media", "Tratamiento programable a corto o mediano plazo."),
    ALTA("Alta", "Atención prioritaria para evitar progreso a daño pulpar o pérdida ósea severa."),
    URGENTE("Urgente", "Atención odontológica inmediata (dolor agudo, absceso periapical o infección activa).");

    private final String etiqueta;
    private final String descripcion;

    NivelUrgencia(String etiqueta, String descripcion) {
        this.etiqueta = etiqueta;
        this.descripcion = descripcion;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public String getDescripcion() {
        return descripcion;
    }
}

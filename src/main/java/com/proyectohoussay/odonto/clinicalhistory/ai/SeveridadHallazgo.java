package com.proyectohoussay.odonto.clinicalhistory.ai;

/**
 * Severity level of an individual finding detected by the AI.
 */
public enum SeveridadHallazgo {
    LEVE("Leve"),
    MODERADA("Moderada"),
    SEVERA("Severa");

    private final String descripcion;

    SeveridadHallazgo(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}

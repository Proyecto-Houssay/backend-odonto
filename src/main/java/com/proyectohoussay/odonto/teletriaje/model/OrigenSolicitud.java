package com.proyectohoussay.odonto.teletriaje.model;

import java.util.Locale;

public enum OrigenSolicitud {
    WEB,
    TELEFONICO,
    PRESENCIAL,
    AUTONOMO,
    DERIVACION;

    public static OrigenSolicitud fromString(String valor) {
        if (valor == null || valor.isBlank()) {
            return WEB;
        }
        try {
            return OrigenSolicitud.valueOf(valor.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return WEB;
        }
    }
}

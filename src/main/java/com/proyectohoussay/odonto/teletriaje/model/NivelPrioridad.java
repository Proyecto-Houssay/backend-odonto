package com.proyectohoussay.odonto.teletriaje.model;

import java.util.Locale;

public enum NivelPrioridad {
    ROJO(1, "Rojo - Urgente / Alta prioridad"),
    AMARILLO(2, "Amarillo - Prioridad media"),
    VERDE(3, "Verde - Prioridad baja / Consulta general");

    private final int orden;
    private final String descripcion;

    NivelPrioridad(int orden, String descripcion) {
        this.orden = orden;
        this.descripcion = descripcion;
    }

    public int getOrden() {
        return orden;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public static NivelPrioridad fromString(String valor) {
        if (valor == null || valor.isBlank()) {
            return VERDE;
        }
        String normalizado = valor.trim().toUpperCase(Locale.ROOT);
        return switch (normalizado) {
            case "ROJO", "ALTA", "URGENTE", "P0" -> ROJO;
            case "AMARILLO", "MEDIA", "MODERADA", "P1" -> AMARILLO;
            case "VERDE", "BAJA", "LEVE", "P2" -> VERDE;
            default -> {
                try {
                    yield NivelPrioridad.valueOf(normalizado);
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Nivel de prioridad no válido: " + valor);
                }
            }
        };
    }
}

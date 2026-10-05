package com.proyectohoussay.odonto.dto;

public record InventarioItemDto(
        Long id,
        String nombre,
        String categoria,
        int cantidadDisponible,
        String unidadMedida,
        String estado
) {
}

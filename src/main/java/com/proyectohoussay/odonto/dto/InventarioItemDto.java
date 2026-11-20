package com.proyectohoussay.odonto.dto;

public record InventarioItemDto(
        Long id,
        String nombre,
        String categoria,
        int cantidadDisponible,
        String unidadMedida,
        int stockMinimo,
        String estado
) {
    public InventarioItemDto(Long id, String nombre, String categoria, int cantidadDisponible,
                             String unidadMedida, String estado) {
        this(id, nombre, categoria, cantidadDisponible, unidadMedida, 0, estado);
    }
}

package com.proyectohoussay.odonto.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InsumoRequest(
        @NotBlank(message = "El nombre del insumo es obligatorio.")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres.")
        String nombre,
        @NotBlank(message = "La categoría del insumo es obligatoria.")
        @Size(max = 50, message = "La categoría no puede superar los 50 caracteres.")
        String categoria,
        @Min(value = 0, message = "La cantidad disponible no puede ser negativa.")
        int cantidadDisponible,
        @NotBlank(message = "La unidad de medida es obligatoria.")
        @Size(max = 20, message = "La unidad de medida no puede superar los 20 caracteres.")
        String unidadMedida,
        @Min(value = 0, message = "El stock mínimo no puede ser negativo.")
        int stockMinimo) {
}

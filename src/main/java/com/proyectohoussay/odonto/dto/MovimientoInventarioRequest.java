package com.proyectohoussay.odonto.dto;

import com.proyectohoussay.odonto.model.TipoMovimientoInventario;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MovimientoInventarioRequest(
        @NotNull TipoMovimientoInventario tipo,
        @Min(value = 1, message = "La cantidad del movimiento debe ser mayor que cero.") int cantidad,
        @NotBlank(message = "El motivo del movimiento es obligatorio.")
        @Size(max = 255, message = "El motivo no puede superar los 255 caracteres.") String motivo
) { }

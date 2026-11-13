package com.proyectohoussay.odonto.dto;

import com.proyectohoussay.odonto.model.TipoMovimientoInventario;

import java.time.LocalDateTime;

public record MovimientoInventarioDto(
        Long id,
        Long insumoId,
        String insumoNombre,
        TipoMovimientoInventario tipo,
        int cantidad,
        int stockAnterior,
        int stockResultante,
        String motivo,
        LocalDateTime fechaHora
) { }

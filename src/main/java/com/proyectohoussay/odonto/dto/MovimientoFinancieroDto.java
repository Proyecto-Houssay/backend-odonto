package com.proyectohoussay.odonto.dto;

import com.proyectohoussay.odonto.model.TipoMovimientoFinanciero;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MovimientoFinancieroDto(
        Long id,
        LocalDate fecha,
        TipoMovimientoFinanciero tipo,
        BigDecimal importe,
        String concepto,
        String medioPago,
        Long turnoId) {
}

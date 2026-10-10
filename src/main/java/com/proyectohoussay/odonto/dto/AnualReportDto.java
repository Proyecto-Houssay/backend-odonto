package com.proyectohoussay.odonto.dto;

import java.math.BigDecimal;
import java.util.List;

public record AnualReportDto(int anio, long totalAtenciones, BigDecimal totalCobrado,
                             long cantidadCobros, List<MesAnualReportDto> meses) {
}

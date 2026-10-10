package com.proyectohoussay.odonto.dto;

import java.math.BigDecimal;

public record MesAnualReportDto(int mes, String nombre, long totalAtenciones,
                                BigDecimal totalCobrado, long cantidadCobros) {
}

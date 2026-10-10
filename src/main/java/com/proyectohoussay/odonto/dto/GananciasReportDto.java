package com.proyectohoussay.odonto.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record GananciasReportDto(LocalDate desde, LocalDate hasta,
                                BigDecimal totalIngresosBrutos, long cantidadCobros) {
}

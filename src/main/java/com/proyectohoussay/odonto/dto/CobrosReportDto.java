package com.proyectohoussay.odonto.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CobrosReportDto(LocalDate desde, LocalDate hasta, BigDecimal totalCobrado,
                              long cantidadCobros, List<PagoResponse> cobros) {
}

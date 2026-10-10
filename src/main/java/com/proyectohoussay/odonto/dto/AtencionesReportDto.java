package com.proyectohoussay.odonto.dto;

import java.time.LocalDate;
import java.util.List;

public record AtencionesReportDto(String tipo, LocalDate desde, LocalDate hasta,
                                  long totalAtenciones, List<AtencionResumenDto> detalle) {
}

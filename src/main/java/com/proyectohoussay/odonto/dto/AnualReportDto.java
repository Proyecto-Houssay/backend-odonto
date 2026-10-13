package com.proyectohoussay.odonto.dto;

import java.util.List;

public record AnualReportDto(int anio, long totalAtenciones, List<MesAnualReportDto> meses) {
}

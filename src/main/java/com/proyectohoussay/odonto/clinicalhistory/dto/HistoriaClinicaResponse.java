package com.proyectohoussay.odonto.clinicalhistory.dto;

import com.proyectohoussay.odonto.clinicalhistory.HistoriaClinica;

import java.time.LocalDate;
import java.util.List;

public record HistoriaClinicaResponse(
        Long id,
        LocalDate fechaApertura,
        Long pacienteId,
        String observaciones,
        List<DiagnosticoDto> diagnosticos
) {
    public static HistoriaClinicaResponse de(HistoriaClinica h) {
        if (h == null) return null;
        List<DiagnosticoDto> diagnosticos = h.getDiagnosticos() != null
                ? h.getDiagnosticos().stream().map(DiagnosticoDto::de).toList()
                : List.of();
        return new HistoriaClinicaResponse(
                h.getId(),
                h.getFechaApertura(),
                h.getPacienteId(),
                h.getObservaciones(),
                diagnosticos
        );
    }
}

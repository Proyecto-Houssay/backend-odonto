package com.proyectohoussay.odonto.clinicalhistory;

import java.time.LocalDate;

public record DiagnosticoResponse(Long id, Long historiaClinicaId, LocalDate fecha, String descripcion) {
    public static DiagnosticoResponse de(Diagnostico diagnosis) {
        return new DiagnosticoResponse(diagnosis.getId(), diagnosis.getHistoriaClinica().getId(),
                diagnosis.getFecha(), diagnosis.getDescripcion());
    }
}

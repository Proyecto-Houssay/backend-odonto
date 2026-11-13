package com.proyectohoussay.odonto.clinicalhistory;

import java.time.LocalDate;

public record HistoriaResponse(Long id, Long pacienteId, LocalDate fechaApertura, String observaciones) {
    public static HistoriaResponse de(HistoriaClinica history) {
        return new HistoriaResponse(history.getId(), history.getPacienteId(),
                history.getFechaApertura(), history.getObservaciones());
    }
}

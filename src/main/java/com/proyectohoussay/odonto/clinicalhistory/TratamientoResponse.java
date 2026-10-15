package com.proyectohoussay.odonto.clinicalhistory;

import java.time.LocalDate;

public record TratamientoResponse(Long id, Long diagnosticoId, String descripcion, LocalDate fecha,
                                  EstadoTratamiento estado) {
    public static TratamientoResponse de(Tratamiento treatment) {
        return new TratamientoResponse(treatment.getId(), treatment.getDiagnostico().getId(),
                treatment.getDescripcion(), treatment.getFecha(), treatment.getEstado());
    }
}

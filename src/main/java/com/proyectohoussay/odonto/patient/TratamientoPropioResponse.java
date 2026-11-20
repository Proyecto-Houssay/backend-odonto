package com.proyectohoussay.odonto.patient;

import com.proyectohoussay.odonto.clinicalhistory.EstadoTratamiento;
import com.proyectohoussay.odonto.clinicalhistory.Tratamiento;

import java.time.LocalDate;

public record TratamientoPropioResponse(Long id, LocalDate fecha, String descripcion, EstadoTratamiento estado) {
    static TratamientoPropioResponse de(Tratamiento tratamiento) {
        return new TratamientoPropioResponse(tratamiento.getId(), tratamiento.getFecha(),
                tratamiento.getDescripcion(), tratamiento.getEstado());
    }
}

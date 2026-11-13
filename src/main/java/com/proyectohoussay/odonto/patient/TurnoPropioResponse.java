package com.proyectohoussay.odonto.patient;

import com.proyectohoussay.odonto.model.Turno;

import java.time.LocalDate;
import java.time.LocalTime;

public record TurnoPropioResponse(Long id, LocalDate fecha, LocalTime hora, String motivo,
                                 String estado, String odontologoNombre) {
    static TurnoPropioResponse de(Turno turno) {
        return new TurnoPropioResponse(turno.getId(), turno.getFecha(), turno.getHora(), turno.getMotivo(),
                turno.getEstado(), turno.getOdontologo().getNombre() + " " + turno.getOdontologo().getApellido());
    }
}

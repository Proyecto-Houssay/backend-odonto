package com.proyectohoussay.odonto.dto;

import com.proyectohoussay.odonto.model.Turno;

import java.time.LocalDate;
import java.time.LocalTime;

public record AtencionResumenDto(Long id, LocalDate fecha, LocalTime hora, String paciente,
                                 String odontologo, String motivo, String estado) {

    public static AtencionResumenDto de(Turno turno) {
        return new AtencionResumenDto(turno.getId(), turno.getFecha(), turno.getHora(),
                turno.getPaciente().getNombre() + " " + turno.getPaciente().getApellido(),
                turno.getOdontologo().getNombre() + " " + turno.getOdontologo().getApellido(),
                turno.getMotivo(), turno.getEstado());
    }
}

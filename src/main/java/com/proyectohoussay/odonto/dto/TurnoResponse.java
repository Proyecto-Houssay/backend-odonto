package com.proyectohoussay.odonto.dto;

import com.proyectohoussay.odonto.model.Odontologo;
import com.proyectohoussay.odonto.model.Turno;
import com.proyectohoussay.odonto.patient.Paciente;

import java.time.LocalDate;
import java.time.LocalTime;

public record TurnoResponse(
        String mensaje,
        Long id,
        LocalDate fecha,
        LocalTime hora,
        String motivo,
        String estado,
        Paciente paciente,
        Odontologo odontologo) {

    public static TurnoResponse de(Turno turno, String mensaje) {
        return new TurnoResponse(
                mensaje,
                turno.getId(),
                turno.getFecha(),
                turno.getHora(),
                turno.getMotivo(),
                turno.getEstado(),
                turno.getPaciente(),
                turno.getOdontologo()
        );
    }
}

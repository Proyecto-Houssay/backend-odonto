package com.proyectohoussay.odonto.teletriaje.dto;

import com.proyectohoussay.odonto.teletriaje.model.CitaTeletriaje;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** Datos mínimos de una consulta virtual para la agenda del equipo clínico. */
public record AgendaTeletriajeResponse(
        Long id,
        Long turnoId,
        LocalDate fecha,
        LocalTime hora,
        String estadoTurno,
        Long pacienteId,
        String pacienteNombre,
        Long odontologoId,
        String odontologoNombre,
        String estadoSala,
        String enlaceVideo,
        LocalDateTime enlaceExpiraEn,
        boolean enlaceFuncional
) {
    public static AgendaTeletriajeResponse de(CitaTeletriaje cita) {
        var turno = cita.getTurno();
        boolean turnoActivo = turno != null && (turno.getEstado() == null || !"CANCELADO".equalsIgnoreCase(turno.getEstado()));
        boolean enlaceFuncional = turnoActivo && cita.isEnlaceFuncional();
        return new AgendaTeletriajeResponse(
                cita.getId(),
                turno != null ? turno.getId() : null,
                turno != null ? turno.getFecha() : null,
                turno != null ? turno.getHora() : null,
                turno != null ? turno.getEstado() : null,
                cita.getPaciente() != null ? cita.getPaciente().getId() : null,
                cita.getPaciente() != null
                        ? cita.getPaciente().getNombre() + " " + cita.getPaciente().getApellido()
                        : null,
                cita.getOdontologo() != null ? cita.getOdontologo().getId() : null,
                cita.getOdontologo() != null
                        ? cita.getOdontologo().getNombre() + " " + cita.getOdontologo().getApellido()
                        : null,
                cita.getEstadoSala() != null ? cita.getEstadoSala().name() : null,
                enlaceFuncional ? cita.getEnlaceVideo() : null,
                cita.getEnlaceExpiraEn(),
                enlaceFuncional
        );
    }
}

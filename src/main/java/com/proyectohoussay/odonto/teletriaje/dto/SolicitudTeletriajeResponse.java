package com.proyectohoussay.odonto.teletriaje.dto;

import com.proyectohoussay.odonto.teletriaje.model.SolicitudTeletriaje;

import java.time.LocalDateTime;

public record SolicitudTeletriajeResponse(
        Long id,
        Long pacienteId,
        String pacienteNombre,
        String pacienteDni,
        String motivoConsulta,
        String descripcionSintomas,
        String prioridadSugerida,
        String prioridadValidada,
        String prioridadEfectiva,
        String estadoValidacion,
        boolean signosAlarma,
        boolean derivacionHumana,
        String origen,
        String estado,
        Long profesionalValidadorId,
        String profesionalValidadorNombre,
        String observacionesClinicas,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaValidacion,
        Long turnoId
) {
    public static SolicitudTeletriajeResponse de(SolicitudTeletriaje s) {
        String pacienteNombre = s.getPaciente() != null
                ? s.getPaciente().getNombre() + " " + s.getPaciente().getApellido()
                : null;
        String pacienteDni = s.getPaciente() != null ? s.getPaciente().getDni() : null;
        Long turnoId = s.getTurno() != null ? s.getTurno().getId() : null;

        return new SolicitudTeletriajeResponse(
                s.getId(),
                s.getPaciente() != null ? s.getPaciente().getId() : null,
                pacienteNombre,
                pacienteDni,
                s.getMotivoConsulta(),
                s.getDescripcionSintomas(),
                s.getPrioridadSugerida() != null ? s.getPrioridadSugerida().name() : null,
                s.getPrioridadValidada() != null ? s.getPrioridadValidada().name() : null,
                s.getPrioridadEfectiva() != null ? s.getPrioridadEfectiva().name() : null,
                s.getEstadoValidacion() != null ? s.getEstadoValidacion().name() : null,
                s.isSignosAlarma(),
                s.isDerivacionHumana(),
                s.getOrigen() != null ? s.getOrigen().name() : null,
                s.getEstado() != null ? s.getEstado().name() : null,
                s.getProfesionalValidadorId(),
                s.getProfesionalValidadorNombre(),
                s.getObservacionesClinicas(),
                s.getFechaCreacion(),
                s.getFechaValidacion(),
                turnoId
        );
    }
}

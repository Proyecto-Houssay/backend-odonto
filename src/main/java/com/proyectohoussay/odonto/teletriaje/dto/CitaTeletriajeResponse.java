package com.proyectohoussay.odonto.teletriaje.dto;

import com.proyectohoussay.odonto.teletriaje.model.CitaTeletriaje;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record CitaTeletriajeResponse(
        Long id,
        Long solicitudId,
        Long turnoId,
        Long pacienteId,
        String pacienteNombre,
        String pacienteDni,
        Long odontologoId,
        String odontologoNombre,
        LocalDate fecha,
        LocalTime hora,
        boolean consentimientoInformado,
        LocalDateTime fechaConsentimiento,
        boolean grabacionHabilitada,
        String estadoSala,
        String enlaceVideo,
        LocalDateTime enlaceExpiraEn,
        boolean enlaceFuncional,
        boolean expirada,
        LocalDateTime fechaCreacion
) {
    public static CitaTeletriajeResponse de(CitaTeletriaje c) {
        String pacienteNombre = c.getPaciente() != null
                ? c.getPaciente().getNombre() + " " + c.getPaciente().getApellido()
                : null;
        String pacienteDni = c.getPaciente() != null ? c.getPaciente().getDni() : null;
        String odontologoNombre = c.getOdontologo() != null
                ? c.getOdontologo().getNombre() + " " + c.getOdontologo().getApellido()
                : null;
        LocalDate fecha = c.getTurno() != null ? c.getTurno().getFecha() : null;
        LocalTime hora = c.getTurno() != null ? c.getTurno().getHora() : null;
        Long solicitudId = c.getSolicitud() != null ? c.getSolicitud().getId() : null;
        Long turnoId = c.getTurno() != null ? c.getTurno().getId() : null;

        return new CitaTeletriajeResponse(
                c.getId(),
                solicitudId,
                turnoId,
                c.getPaciente() != null ? c.getPaciente().getId() : null,
                pacienteNombre,
                pacienteDni,
                c.getOdontologo() != null ? c.getOdontologo().getId() : null,
                odontologoNombre,
                fecha,
                hora,
                c.isConsentimientoInformado(),
                c.getFechaConsentimiento(),
                c.isGrabacionHabilitada(),
                c.getEstadoSala() != null ? c.getEstadoSala().name() : null,
                c.getEnlaceVideo(),
                c.getEnlaceExpiraEn(),
                c.isEnlaceFuncional(),
                c.estaExpirada(),
                c.getFechaCreacion()
        );
    }
}

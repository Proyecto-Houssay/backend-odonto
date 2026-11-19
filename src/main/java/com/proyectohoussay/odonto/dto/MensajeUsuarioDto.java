package com.proyectohoussay.odonto.dto;

import com.proyectohoussay.odonto.model.MensajeUsuario;

import java.time.LocalDateTime;

public record MensajeUsuarioDto(
        Long id,
        Long destinatarioId,
        Long remitenteId,
        String remitenteNombre,
        String asunto,
        String contenido,
        boolean leido,
        LocalDateTime creadoEn,
        LocalDateTime leidoEn
) {
    public static MensajeUsuarioDto de(MensajeUsuario mensaje) {
        return new MensajeUsuarioDto(
                mensaje.getId(),
                mensaje.getDestinatarioId(),
                mensaje.getRemitenteId(),
                mensaje.getRemitenteNombre(),
                mensaje.getAsunto(),
                mensaje.getContenido(),
                mensaje.isLeido(),
                mensaje.getCreadoEn(),
                mensaje.getLeidoEn()
        );
    }
}

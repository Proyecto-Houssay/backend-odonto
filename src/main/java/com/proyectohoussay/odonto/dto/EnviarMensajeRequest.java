package com.proyectohoussay.odonto.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EnviarMensajeRequest(
        Long destinatarioId,
        String destinatarioUsername,
        @NotBlank(message = "El asunto es obligatorio.")
        @Size(max = 160, message = "El asunto no puede superar los 160 caracteres.")
        String asunto,
        @NotBlank(message = "El contenido del mensaje es obligatorio.")
        @Size(max = 1000, message = "El contenido no puede superar los 1000 caracteres.")
        String contenido
) {
}

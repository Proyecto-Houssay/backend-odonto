package com.proyectohoussay.odonto.chatbot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO de entrada para el endpoint del chatbot.
 * Contiene el mensaje del usuario y un ID de sesi??n opcional para contexto.
 */
public record ChatRequest(
    @NotBlank(message = "El mensaje no puede estar vac??o")
    @Size(max = 1000, message = "El mensaje no puede superar los 1000 caracteres")
    String mensaje,

    String sessionId
) {}

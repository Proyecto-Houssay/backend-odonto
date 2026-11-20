package com.proyectohoussay.odonto.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ActualizarPerfilRequest(
        @NotBlank(message = "El nombre para mostrar es obligatorio.")
        @Size(max = 120, message = "El nombre para mostrar no puede superar los 120 caracteres.")
        String nombreMostrar,
        String fotoBase64
) {
}

package com.proyectohoussay.odonto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "El usuario o correo electrónico es obligatorio")
        String usernameOrEmail,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(max = 72, message = "La contraseña supera el máximo permitido")
        String password
) {
}

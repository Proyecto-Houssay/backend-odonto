package com.proyectohoussay.odonto.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "El usuario o correo electrónico es obligatorio")
        String usernameOrEmail,

        @NotBlank(message = "La contraseña es obligatoria")
        String password
) {
}

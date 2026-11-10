package com.proyectohoussay.odonto.auth;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetRequest(
        @JsonAlias("email")
        @NotBlank(message = "El usuario o correo electrónico es obligatorio.")
        @Size(max = 254)
        String identifier
) {
}

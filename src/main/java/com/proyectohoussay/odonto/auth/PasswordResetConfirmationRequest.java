package com.proyectohoussay.odonto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetConfirmationRequest(
        @NotBlank(message = "El token de recuperación es obligatorio.") String token,
        @NotBlank(message = "La nueva contraseña es obligatoria.")
        @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres.")
        String newPassword
) {
}

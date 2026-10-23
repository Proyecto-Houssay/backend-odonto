package com.proyectohoussay.odonto.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

/** Update profile payload. Password changes are deliberately not supported here. */
public record UsuarioUpdateRequest(
        @NotBlank(message = "El nombre de usuario es obligatorio.")
        @Size(max = 50, message = "El nombre de usuario no puede superar 50 caracteres.")
        String username,
        @NotBlank(message = "El nombre es obligatorio.")
        @Size(max = 100)
        String nombre,
        @NotBlank(message = "El apellido es obligatorio.")
        @Size(max = 100)
        String apellido,
        @NotBlank(message = "El email es obligatorio.")
        @Email(message = "El email no tiene un formato válido.")
        @Size(max = 254)
        String email,
        @NotBlank(message = "El rol es obligatorio.")
        @Size(max = 30)
        @Pattern(regexp = "(?i)ADMINISTRADOR|ODONTOLOGO|RECEPCIONISTA", message = "El rol no es válido.")
        String rol,
        @NotNull(message = "El estado activo es obligatorio.")
        Boolean activo,
        String telefono) {
}

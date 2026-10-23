package com.proyectohoussay.odonto.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UsuarioCreateRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        String nombre,

        @NotBlank(message = "El apellido es obligatorio")
        @Size(max = 100, message = "El apellido no puede superar los 100 caracteres")
        String apellido,

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no tiene un formato válido")
        @Size(max = 255, message = "El email no puede superar los 255 caracteres")
        String email,

        @Size(max = 50, message = "El usuario no puede superar los 50 caracteres")
        String username,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(max = 72, message = "La contraseña supera el máximo permitido")
        String password,

        String rol,
        Boolean activo,
        String telefono
) {
}

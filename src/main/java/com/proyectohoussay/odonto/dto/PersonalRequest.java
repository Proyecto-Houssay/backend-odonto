package com.proyectohoussay.odonto.dto;

import com.proyectohoussay.odonto.model.CargoPersonal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

public record PersonalRequest(
        @NotBlank(message = "El nombre es obligatorio.")
        @Size(max = 100, message = "El nombre no puede superar 100 caracteres.")
        String nombre,

        @NotBlank(message = "El apellido es obligatorio.")
        @Size(max = 100, message = "El apellido no puede superar 100 caracteres.")
        String apellido,

        @NotBlank(message = "El DNI es obligatorio.")
        @Size(max = 20, message = "El DNI no puede superar 20 caracteres.")
        String dni,

        @Size(max = 100, message = "El email no puede superar 100 caracteres.")
        @Email(message = "El email debe tener un formato válido.")
        String email,

        @Size(max = 20, message = "El teléfono no puede superar 20 caracteres.")
        @Pattern(regexp = "^[+0-9() .-]*$", message = "El teléfono contiene caracteres no válidos.")
        String telefono,

        @NotNull(message = "El cargo es obligatorio.")
        CargoPersonal cargo,

        LocalDate fechaIngreso,

        @Size(max = 100, message = "El horario laboral no puede superar 100 caracteres.")
        String horarioLaboral,

        Long usuarioId,

        Boolean activo
) {
}

package com.proyectohoussay.odonto.patient;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record PacienteRequest(
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 255) String nombre,
        @NotBlank(message = "El apellido es obligatorio") @Size(max = 255) String apellido,
        @NotBlank(message = "El DNI es obligatorio") @Size(max = 255) String dni,
        @Size(max = 255) String telefono,
        @NotBlank(message = "El correo electrónico es obligatorio")
        @Email(message = "El correo electrónico no tiene un formato válido") @Size(max = 255) String email,
        @NotNull(message = "La fecha de nacimiento es obligatoria")
        @PastOrPresent(message = "La fecha de nacimiento no puede ser futura") LocalDate fechaNacimiento
) {
    public Paciente toEntity() {
        Paciente paciente = new Paciente();
        paciente.setNombre(nombre);
        paciente.setApellido(apellido);
        paciente.setDni(dni);
        paciente.setTelefono(telefono);
        paciente.setEmail(email);
        paciente.setFechaNacimiento(fechaNacimiento);
        return paciente;
    }
}

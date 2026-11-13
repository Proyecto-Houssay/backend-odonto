package com.proyectohoussay.odonto.dto;

import com.proyectohoussay.odonto.model.CargoPersonal;
import com.proyectohoussay.odonto.model.Personal;

import java.time.LocalDate;

public record PersonalResponse(
        Long id,
        String nombre,
        String apellido,
        String dni,
        String email,
        String telefono,
        CargoPersonal cargo,
        String cargoLegible,
        LocalDate fechaIngreso,
        String horarioLaboral,
        boolean activo,
        Long usuarioId
) {
    public static PersonalResponse de(Personal p) {
        if (p == null) return null;
        return new PersonalResponse(
                p.getId(),
                p.getNombre(),
                p.getApellido(),
                p.getDni(),
                p.getEmail(),
                p.getTelefono(),
                p.getCargo(),
                p.getCargo() != null ? p.getCargo().getNombreLegible() : null,
                p.getFechaIngreso(),
                p.getHorarioLaboral(),
                p.isActivo(),
                p.getUsuarioId()
        );
    }
}

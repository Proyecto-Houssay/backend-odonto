package com.proyectohoussay.odonto.dto;

import com.proyectohoussay.odonto.model.Usuario;

public record UserProfileResponse(
        Long id,
        String username,
        String nombre,
        String apellido,
        String email,
        String rol,
        boolean activo,
        String telefono,
        PerfilVinculadoDto perfilVinculado
) {
    public record PerfilVinculadoDto(
            String tipo, // "ODONTOLOGO", "PERSONAL", "PACIENTE", "ADMINISTRADOR"
            Long perfilId,
            String cargo,
            String matricula,
            String especialidad
    ) {
    }

    public static UserProfileResponse de(Usuario usuario, PerfilVinculadoDto perfil) {
        if (usuario == null) return null;
        return new UserProfileResponse(
                usuario.getId(),
                usuario.getUsername(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getEmail(),
                usuario.getRol(),
                usuario.isActivo(),
                usuario.getTelefono(),
                perfil
        );
    }
}

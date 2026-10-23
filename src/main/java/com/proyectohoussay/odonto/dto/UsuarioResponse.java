package com.proyectohoussay.odonto.dto;

import com.proyectohoussay.odonto.model.Usuario;

public record UsuarioResponse(
        Long id,
        String username,
        String nombre,
        String apellido,
        String email,
        String rol,
        boolean activo,
        String telefono) {

    public static UsuarioResponse de(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getUsername(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getEmail(),
                usuario.getRol(),
                usuario.isActivo(),
                usuario.getTelefono());
    }
}

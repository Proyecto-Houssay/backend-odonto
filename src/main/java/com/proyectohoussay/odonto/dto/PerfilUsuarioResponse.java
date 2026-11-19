package com.proyectohoussay.odonto.dto;

import com.proyectohoussay.odonto.model.Usuario;

public record PerfilUsuarioResponse(
        Long id,
        String username,
        String nombreMostrar,
        String email,
        String rol,
        String fotoBase64
) {
    public static PerfilUsuarioResponse de(Usuario usuario) {
        String display = usuario.getNombreMostrar();
        if (display == null || display.isBlank()) {
            String fullName = ((usuario.getNombre() != null ? usuario.getNombre() : "")
                    + " "
                    + (usuario.getApellido() != null ? usuario.getApellido() : "")).trim();
            display = fullName.isEmpty() ? usuario.getUsername() : fullName;
        }
        return new PerfilUsuarioResponse(
                usuario.getId(),
                usuario.getUsername(),
                display,
                usuario.getEmail(),
                usuario.getRol(),
                usuario.getFotoBase64()
        );
    }
}

package com.proyectohoussay.odonto.dto;

public record DirectorioItemDto(
        String tipo, Long identificador, String nombreCompleto, String dni, String telefono,
        String email, String detalle, boolean activo
) {}

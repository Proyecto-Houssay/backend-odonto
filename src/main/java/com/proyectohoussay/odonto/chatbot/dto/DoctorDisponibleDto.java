package com.proyectohoussay.odonto.chatbot.dto;

public record DoctorDisponibleDto(
        Long id,
        String nombre,
        String apellido,
        String nombreCompleto,
        String especialidad
) {
}

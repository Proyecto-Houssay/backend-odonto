package com.proyectohoussay.odonto.auth;

public record AuthResponse(String token, String tokenType, long expiresIn) {
}

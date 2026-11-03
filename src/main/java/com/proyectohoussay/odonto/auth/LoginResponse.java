package com.proyectohoussay.odonto.auth;

public record LoginResponse(String token, String role, long expiresInSeconds) { }

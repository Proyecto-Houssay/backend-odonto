package com.proyectohoussay.odonto.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Optional;

@Service
public class AuthService {
    private final JwtTokenService tokenService;

    @Value("${app.auth.admin.username:}") private String adminUsername;
    @Value("${app.auth.admin.password:}") private String adminPassword;
    @Value("${app.auth.patient.username:}") private String patientUsername;
    @Value("${app.auth.patient.password:}") private String patientPassword;
    @Value("${app.auth.odontologo.username:}") private String odontologoUsername;
    @Value("${app.auth.odontologo.password:}") private String odontologoPassword;
    @Value("${app.auth.recepcionista.username:}") private String recepcionistaUsername;
    @Value("${app.auth.recepcionista.password:}") private String recepcionistaPassword;

    public AuthService(JwtTokenService tokenService) {
        this.tokenService = tokenService;
    }

    public Optional<LoginResponse> authenticate(String username, String password) {
        if (username == null || password == null) return Optional.empty();
        List<Credential> configured = List.of(
                new Credential(adminUsername, adminPassword, "ADMIN"),
                new Credential(patientUsername, patientPassword, "PATIENT"),
                new Credential(odontologoUsername, odontologoPassword, "ODONTOLOGO"),
                new Credential(recepcionistaUsername, recepcionistaPassword, "RECEPCIONISTA")
        );
        return configured.stream()
                .filter(credential -> matches(credential.username(), username)
                        && matches(credential.password(), password))
                .findFirst()
                .map(credential -> new LoginResponse(tokenService.issue(username, credential.role()),
                        credential.role(), tokenService.lifetimeSeconds()));
    }

    private boolean matches(String configured, String provided) {
        if (configured == null || configured.isBlank() || provided == null) return false;
        return MessageDigest.isEqual(configured.getBytes(StandardCharsets.UTF_8), provided.getBytes(StandardCharsets.UTF_8));
    }

    private record Credential(String username, String password, String role) { }
}

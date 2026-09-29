package com.proyectohoussay.odonto.auth;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {

        boolean authenticated = authService.authenticate(
                request.usernameOrEmail(),
                request.password()
        );

        if (!authenticated) {
            return ResponseEntity.status(401)
                    .body("Credenciales incorrectas");
        }

        return ResponseEntity.ok("Inicio de sesión exitoso");
    }
}
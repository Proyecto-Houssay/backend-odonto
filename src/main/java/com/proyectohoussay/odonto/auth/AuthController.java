package com.proyectohoussay.odonto.auth;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {

        var session = authService.authenticate(
                request.usernameOrEmail(),
                request.password()
        );

        if (session.isEmpty()) {
            return ResponseEntity.status(401)
                    .body(java.util.Map.of("message", "Credenciales incorrectas"));
        }

        return ResponseEntity.ok(session.get());
    }
}

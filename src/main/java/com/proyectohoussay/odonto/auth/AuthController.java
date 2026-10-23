package com.proyectohoussay.odonto.auth;

import jakarta.validation.Valid;
import com.proyectohoussay.odonto.model.Usuario;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtTokenService jwtTokenService;
    private final AccountService accountService;

    public AuthController(AuthService authService, JwtTokenService jwtTokenService, AccountService accountService) {
        this.authService = authService;
        this.jwtTokenService = jwtTokenService;
        this.accountService = accountService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {

        Optional<Usuario> authenticatedUser = authService.authenticate(
                request.usernameOrEmail(),
                request.password()
        );

        if (authenticatedUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthErrorResponse("Credenciales incorrectas."));
        }

        return ResponseEntity.ok(jwtTokenService.issueToken(authenticatedUser.get()));
    }

    @PostMapping("/register")
    public ResponseEntity<RegistrationResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accountService.register(request));
    }

    @PostMapping("/password-reset/request")
    public ResponseEntity<PasswordResetRequestResponse> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequest request) {
        return ResponseEntity.ok(accountService.requestPasswordReset(request.email()));
    }

    @PostMapping("/password-reset/confirm")
    public ResponseEntity<Void> confirmPasswordReset(
            @Valid @RequestBody PasswordResetConfirmationRequest request) {
        accountService.confirmPasswordReset(request.token(), request.newPassword());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/change-password")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal Jwt jwt,
                                               @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(jwt.getSubject(), request.currentPassword(), request.newPassword());
        return ResponseEntity.noContent().build();
    }
}

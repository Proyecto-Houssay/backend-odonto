package com.proyectohoussay.odonto.auth;

import com.proyectohoussay.odonto.model.PasswordResetToken;
import com.proyectohoussay.odonto.model.Usuario;
import com.proyectohoussay.odonto.repository.PasswordResetTokenRepository;
import com.proyectohoussay.odonto.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

@Service
public class AccountService {
    private static final Duration RESET_TOKEN_LIFETIME = Duration.ofMinutes(30);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int MAX_BCRYPT_PASSWORD_BYTES = 72;

    private final UsuarioRepository usuarioRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccountEmailService emailService;
    private final String frontendBaseUrl;

    public AccountService(
            UsuarioRepository usuarioRepository,
            PasswordResetTokenRepository resetTokenRepository,
            PasswordEncoder passwordEncoder,
            AccountEmailService emailService,
            @Value("${app.frontend-base-url:http://localhost:5174}") String frontendBaseUrl) {
        this.usuarioRepository = usuarioRepository;
        this.resetTokenRepository = resetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.frontendBaseUrl = frontendBaseUrl.replaceAll("/+$", "");
    }

    @Transactional
    public RegistrationResponse register(RegisterRequest request) {
        String username = request.username().trim().toLowerCase(Locale.ROOT);
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        validatePassword(request.password());
        if (usuarioRepository.existsByUsernameIgnoreCase(username) || usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateAccountException();
        }

        Usuario user = new Usuario(username, request.nombre().trim(), request.apellido().trim(), email,
                passwordEncoder.encode(request.password()), UserRole.RECEPCIONISTA.name(), true);
        try {
            user = usuarioRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateAccountException();
        }

        boolean emailSent = emailService.sendRegistrationConfirmation(email, user.getNombre());
        return emailSent
                ? new RegistrationResponse(true, "Cuenta creada. Se envió un correo de confirmación.")
                : new RegistrationResponse(false, "Cuenta creada. El correo no se envió porque SMTP no está configurado o no está disponible.");
    }

    @Transactional
    public PasswordResetRequestResponse requestPasswordReset(String email) {
        boolean emailConfigured = emailService.isConfigured();
        if (emailConfigured) {
            usuarioRepository.findByEmailIgnoreCase(email.trim())
                    .filter(Usuario::isActivo)
                    .ifPresent(user -> {
                        resetTokenRepository.deleteAllByUsuario(user);
                        String rawToken = createRawToken();
                        String tokenHash = hashToken(rawToken);
                        resetTokenRepository.save(new PasswordResetToken(user, tokenHash, Instant.now().plus(RESET_TOKEN_LIFETIME)));
                        String resetUrl = frontendBaseUrl + "/?token=" + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
                        if (!emailService.sendPasswordReset(user.getEmail(), user.getNombre(), resetUrl)) {
                            resetTokenRepository.deleteAllByUsuario(user);
                        }
                    });
        }
        return new PasswordResetRequestResponse(
                "Si el correo corresponde a una cuenta activa y el envío está disponible, vas a recibir un enlace de recuperación.",
                emailConfigured);
    }

    @Transactional
    public void confirmPasswordReset(String rawToken, String newPassword) {
        validatePassword(newPassword);
        PasswordResetToken resetToken = resetTokenRepository.findByHashForUpdate(hashToken(rawToken))
                .filter(token -> token.getUsedAt() == null && token.getExpiresAt().isAfter(Instant.now())
                        && token.getUsuario().isActivo())
                .orElseThrow(InvalidPasswordResetTokenException::new);

        Usuario user = resetToken.getUsuario();
        user.setPassword(passwordEncoder.encode(newPassword));
        usuarioRepository.save(user);
        resetTokenRepository.deleteAllByUsuario(user);
    }

    private static void validatePassword(String password) {
        if (password == null || password.isBlank() || password.length() < 8
                || password.getBytes(StandardCharsets.UTF_8).length > MAX_BCRYPT_PASSWORD_BYTES) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 8 caracteres y no superar 72 bytes UTF-8.");
        }
    }

    private static String createRawToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hashToken(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available.", exception);
        }
    }
}

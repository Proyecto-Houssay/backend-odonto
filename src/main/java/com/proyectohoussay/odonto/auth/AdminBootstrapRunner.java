package com.proyectohoussay.odonto.auth;

import com.proyectohoussay.odonto.model.Usuario;
import com.proyectohoussay.odonto.repository.UsuarioRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Component
@Profile("bootstrap-admin")
public class AdminBootstrapRunner implements ApplicationRunner {

    private static final int MAX_BCRYPT_PASSWORD_BYTES = 72;

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    public AdminBootstrapRunner(UsuarioRepository usuarioRepository,
                                PasswordEncoder passwordEncoder,
                                Environment environment) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (usuarioRepository.existsByRolIgnoreCase(UserRole.ADMINISTRADOR.name())) {
            return;
        }

        String username = requiredEnvironmentValue("BOOTSTRAP_ADMIN_USERNAME").trim().toLowerCase(Locale.ROOT);
        String email = requiredEnvironmentValue("BOOTSTRAP_ADMIN_EMAIL").trim().toLowerCase(Locale.ROOT);
        String rawPassword = requiredEnvironmentValue("BOOTSTRAP_ADMIN_PASSWORD");
        if (username.length() > 50 || email.length() > 254 || !email.contains("@")) {
            throw new IllegalStateException("Bootstrap administrator username or email is invalid.");
        }
        if (rawPassword.isBlank() || rawPassword.getBytes(StandardCharsets.UTF_8).length > MAX_BCRYPT_PASSWORD_BYTES) {
            throw new IllegalStateException("BOOTSTRAP_ADMIN_PASSWORD must be non-blank and at most 72 UTF-8 bytes.");
        }
        if (usuarioRepository.existsByUsernameIgnoreCase(username) || usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalStateException("Bootstrap administrator username or email is already in use.");
        }

        Usuario administrator = new Usuario(username, "Administrator", "Novadent", email,
                passwordEncoder.encode(rawPassword), UserRole.ADMINISTRADOR.name(), true);
        usuarioRepository.save(administrator);
    }

    private String requiredEnvironmentValue(String name) {
        String value = environment.getProperty(name);
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException("Missing required environment variable: " + name);
        }
        return value;
    }
}

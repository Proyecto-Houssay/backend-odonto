package com.proyectohoussay.odonto.auth;

import com.proyectohoussay.odonto.model.Usuario;
import com.proyectohoussay.odonto.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Service
public class AuthService {

    private static final int MAX_BCRYPT_PASSWORD_BYTES = 72;

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public boolean authenticate(String usernameOrEmail, String password) {
        if (usernameOrEmail == null || usernameOrEmail.isBlank()
                || password == null || password.isBlank()
                || password.getBytes(StandardCharsets.UTF_8).length > MAX_BCRYPT_PASSWORD_BYTES) {
            return false;
        }

        String identifier = usernameOrEmail.trim();
        Optional<Usuario> usuario = usuarioRepository.findByUsernameIgnoreCase(identifier)
                .or(() -> usuarioRepository.findByEmailIgnoreCase(identifier));

        return usuario
                .filter(Usuario::isActivo)
                .map(Usuario::getPasswordHash)
                .filter(hash -> hash != null && !hash.isBlank())
                .map(hash -> matches(password, hash))
                .orElse(false);
    }

    private boolean matches(String rawPassword, String passwordHash) {
        try {
            return passwordEncoder.matches(rawPassword, passwordHash);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}

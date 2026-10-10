package com.proyectohoussay.odonto.auth;

import com.proyectohoussay.odonto.model.Usuario;
import com.proyectohoussay.odonto.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public boolean authenticate(String usernameOrEmail, String password) {

        if (usernameOrEmail == null || usernameOrEmail.isBlank()) {
            return false;
        }

        if (password == null || password.isBlank()) {
            return false;
        }

        Optional<Usuario> usuario = usuarioRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail);
        return usuario
                .filter(Usuario::isActivo)
                .map(user -> passwordEncoder.matches(password, user.getPassword()))
                .orElse(false);
    }
}

package com.proyectohoussay.odonto.service;

import com.proyectohoussay.odonto.model.Usuario;
import com.proyectohoussay.odonto.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class UsuarioService {

    private static final int MAX_BCRYPT_PASSWORD_BYTES = 72;

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<Usuario> listarUsuarios() {
        return usuarioRepository.findAll();
    }

    public Usuario obtenerUsuario(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con ID: " + id));
    }

    public Optional<Usuario> buscarPorEmail(String email) {
        return usuarioRepository.findByEmail(email);
    }

    public Usuario crearUsuario(Usuario usuario, String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("La contraseña es obligatoria.");
        }
        if (rawPassword.getBytes(StandardCharsets.UTF_8).length > MAX_BCRYPT_PASSWORD_BYTES) {
            throw new IllegalArgumentException("La contraseña supera el máximo permitido.");
        }

        String email = normalizeRequired(usuario.getEmail());
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("El email ya se encuentra registrado: " + email);
        }

        usuario.setEmail(email);
        usuario.setUsername(normalizeOptional(usuario.getUsername()));
        if (usuario.getUsername() != null && usuarioRepository.existsByUsernameIgnoreCase(usuario.getUsername())) {
            throw new IllegalArgumentException("El nombre de usuario ya se encuentra registrado.");
        }

        usuario.setPasswordHash(passwordEncoder.encode(rawPassword));
        return usuarioRepository.save(usuario);
    }

    public Usuario actualizarUsuario(Long id, Usuario usuarioDetails) {
        Usuario usuario = obtenerUsuario(id);
        usuario.setNombre(usuarioDetails.getNombre());
        usuario.setApellido(usuarioDetails.getApellido());
        usuario.setEmail(usuarioDetails.getEmail());
        usuario.setUsername(normalizeOptional(usuarioDetails.getUsername()));
        usuario.setRol(usuarioDetails.getRol());
        usuario.setActivo(usuarioDetails.isActivo());
        if (usuarioDetails.getTelefono() != null) {
            usuario.setTelefono(usuarioDetails.getTelefono());
        }
        return usuarioRepository.save(usuario);
    }

    public void eliminarUsuario(Long id) {
        Usuario usuario = obtenerUsuario(id);
        usuarioRepository.delete(usuario);
    }

    private String normalizeRequired(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("El email es obligatorio.");
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}

package com.proyectohoussay.odonto.service;

import com.proyectohoussay.odonto.model.Usuario;
import com.proyectohoussay.odonto.repository.UsuarioRepository;
import com.proyectohoussay.odonto.exception.UsuarioDuplicadoException;
import com.proyectohoussay.odonto.dto.UsuarioUpdateRequest;
import com.proyectohoussay.odonto.auth.UserRole;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public List<Usuario> listarUsuarios() {
        return usuarioRepository.findAll();
    }

    public Usuario obtenerUsuario(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con ID: " + id));
    }

    public Optional<Usuario> buscarPorEmail(String email) {
        return usuarioRepository.findByEmailIgnoreCase(email);
    }

    public Usuario crearUsuario(Usuario usuario) {
        usuario.setUsername(normalizeRequired(usuario.getUsername(), "El nombre de usuario es obligatorio."));
        usuario.setEmail(normalizeRequired(usuario.getEmail(), "El email es obligatorio."));
        usuario.setRol(normalizeRole(usuario.getRol()));
        validarUnicidad(usuario.getUsername(), usuario.getEmail(), null);
        try {
            return usuarioRepository.save(usuario);
        } catch (DataIntegrityViolationException exception) {
            throw new UsuarioDuplicadoException("El nombre de usuario o el email ya se encuentran registrados.");
        }
    }

    public Usuario actualizarUsuario(Long id, UsuarioUpdateRequest usuarioDetails) {
        Usuario usuario = obtenerUsuario(id);
        String rol = normalizeRole(usuarioDetails.rol());
        String username = normalizeRequired(usuarioDetails.username(), "El nombre de usuario es obligatorio.");
        String email = normalizeRequired(usuarioDetails.email(), "El email es obligatorio.");
        validarUnicidad(username, email, id);
        usuario.setUsername(username);
        usuario.setNombre(usuarioDetails.nombre());
        usuario.setApellido(usuarioDetails.apellido());
        usuario.setEmail(email);
        usuario.setRol(rol);
        usuario.setActivo(usuarioDetails.activo());
        usuario.setTelefono(usuarioDetails.telefono());
        try {
            return usuarioRepository.save(usuario);
        } catch (DataIntegrityViolationException exception) {
            throw new UsuarioDuplicadoException("El nombre de usuario o el email ya se encuentran registrados.");
        }
    }

    private void validarUnicidad(String username, String email, Long usuarioId) {
        if (username != null && !username.isBlank()) {
            boolean duplicado = usuarioId == null
                    ? usuarioRepository.existsByUsernameIgnoreCase(username)
                    : usuarioRepository.existsByUsernameIgnoreCaseAndIdNot(username, usuarioId);
            if (duplicado) {
                throw new UsuarioDuplicadoException("El nombre de usuario ya se encuentra registrado.");
            }
        } else if (username != null) {
            throw new IllegalArgumentException("El nombre de usuario es obligatorio.");
        }

        if (email != null && !email.isBlank()) {
            boolean duplicado = usuarioId == null
                    ? usuarioRepository.existsByEmailIgnoreCase(email)
                    : usuarioRepository.existsByEmailIgnoreCaseAndIdNot(email, usuarioId);
            if (duplicado) {
                throw new UsuarioDuplicadoException("El email ya se encuentra registrado.");
            }
        }
    }

    private String normalizeRole(String role) {
        return UserRole.from(role)
                .map(UserRole::name)
                .orElseThrow(() -> new IllegalArgumentException("El rol no es válido."));
    }

    private String normalizeRequired(String value, String errorMessage) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(errorMessage);
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    public void eliminarUsuario(Long id) {
        Usuario usuario = obtenerUsuario(id);
        usuarioRepository.delete(usuario);
    }
}

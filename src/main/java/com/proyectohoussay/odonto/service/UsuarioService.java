package com.proyectohoussay.odonto.service;

import com.proyectohoussay.odonto.model.Usuario;
import com.proyectohoussay.odonto.repository.UsuarioRepository;
import com.proyectohoussay.odonto.exception.UsuarioDuplicadoException;
import com.proyectohoussay.odonto.dto.UsuarioUpdateRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;
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
        return usuarioRepository.findByEmail(email);
    }

    public Usuario crearUsuario(Usuario usuario) {
        validarUnicidad(usuario.getUsername(), usuario.getEmail(), null);
        try {
            return usuarioRepository.save(usuario);
        } catch (DataIntegrityViolationException exception) {
            throw new UsuarioDuplicadoException("El nombre de usuario o el email ya se encuentran registrados.");
        }
    }

    public Usuario actualizarUsuario(Long id, UsuarioUpdateRequest usuarioDetails) {
        Usuario usuario = obtenerUsuario(id);
        validarUnicidad(usuarioDetails.username(), usuarioDetails.email(), id);
        usuario.setUsername(usuarioDetails.username());
        usuario.setNombre(usuarioDetails.nombre());
        usuario.setApellido(usuarioDetails.apellido());
        usuario.setEmail(usuarioDetails.email());
        usuario.setRol(usuarioDetails.rol());
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
                    ? usuarioRepository.existsByUsername(username)
                    : usuarioRepository.existsByUsernameAndIdNot(username, usuarioId);
            if (duplicado) {
                throw new UsuarioDuplicadoException("El nombre de usuario ya se encuentra registrado.");
            }
        } else if (username != null) {
            throw new IllegalArgumentException("El nombre de usuario es obligatorio.");
        }

        if (email != null && !email.isBlank()) {
            boolean duplicado = usuarioId == null
                    ? usuarioRepository.existsByEmail(email)
                    : usuarioRepository.existsByEmailAndIdNot(email, usuarioId);
            if (duplicado) {
                throw new UsuarioDuplicadoException("El email ya se encuentra registrado.");
            }
        }
    }

    public void eliminarUsuario(Long id) {
        Usuario usuario = obtenerUsuario(id);
        usuarioRepository.delete(usuario);
    }
}

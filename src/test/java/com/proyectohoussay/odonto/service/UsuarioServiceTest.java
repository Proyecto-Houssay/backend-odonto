package com.proyectohoussay.odonto.service;

import com.proyectohoussay.odonto.dto.UsuarioUpdateRequest;
import com.proyectohoussay.odonto.exception.UsuarioDuplicadoException;
import com.proyectohoussay.odonto.model.Usuario;
import com.proyectohoussay.odonto.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UsuarioServiceTest {

    private static final String STORED_HASH = "$2a$10$4V9v9vIf5fWGQU1kX5lwIuF6vo5zBipNL9V4sBcYt8PhE9Vwq4v6K";

    private UsuarioRepository usuarioRepository;
    private UsuarioService usuarioService;

    @BeforeEach
    void setUp() {
        usuarioRepository = mock(UsuarioRepository.class);
        usuarioService = new UsuarioService(usuarioRepository);
    }

    @Test
    void rejectsDuplicateUsernameBeforeSaving() {
        Usuario newUser = usuario("admin", "admin@example.com", "password");
        when(usuarioRepository.existsByUsernameIgnoreCase("admin")).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.crearUsuario(newUser))
                .isInstanceOf(UsuarioDuplicadoException.class)
                .hasMessage("El nombre de usuario ya se encuentra registrado.");

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void rejectsDuplicateEmailBeforeSaving() {
        Usuario newUser = usuario("admin", "admin@example.com", "password");
        when(usuarioRepository.existsByUsernameIgnoreCase("admin")).thenReturn(false);
        when(usuarioRepository.existsByEmailIgnoreCase("admin@example.com")).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.crearUsuario(newUser))
                .isInstanceOf(UsuarioDuplicadoException.class)
                .hasMessage("El email ya se encuentra registrado.");

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void updateProfileCannotChangeOrClearStoredPassword() {
        Usuario current = usuario("admin", "admin@example.com", STORED_HASH);
        current.setId(7L);
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(current));
        when(usuarioRepository.existsByUsernameIgnoreCaseAndIdNot("admin_updated", 7L)).thenReturn(false);
        when(usuarioRepository.existsByEmailIgnoreCaseAndIdNot("updated@example.com", 7L)).thenReturn(false);
        when(usuarioRepository.save(current)).thenReturn(current);

        UsuarioUpdateRequest request = new UsuarioUpdateRequest(
                "admin_updated", "Administrador", "Clinica", "updated@example.com", "ADMINISTRADOR", true, null);
        Usuario updated = usuarioService.actualizarUsuario(7L, request);

        assertThat(updated.getPassword()).isEqualTo(STORED_HASH);
        assertThat(updated.getUsername()).isEqualTo("admin_updated");
    }

    @Test
    void rejectsDuplicateUsernameOnUpdateBeforeSaving() {
        Usuario current = usuario("admin", "admin@example.com", STORED_HASH);
        current.setId(7L);
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(current));
        when(usuarioRepository.existsByUsernameIgnoreCaseAndIdNot("taken", 7L)).thenReturn(true);

        UsuarioUpdateRequest request = new UsuarioUpdateRequest(
                "taken", "Administrador", "Clinica", "admin@example.com", "ADMINISTRADOR", true, null);

        assertThatThrownBy(() -> usuarioService.actualizarUsuario(7L, request))
                .isInstanceOf(UsuarioDuplicadoException.class);
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    private Usuario usuario(String username, String email, String password) {
        return new Usuario(username, "Administrador", "Clinica", email, password, "ADMINISTRADOR", true);
    }
}

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
        Usuario newUser = usuario("iris", "iris@example.com", "password");
        when(usuarioRepository.existsByUsername("iris")).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.crearUsuario(newUser))
                .isInstanceOf(UsuarioDuplicadoException.class)
                .hasMessage("El nombre de usuario ya se encuentra registrado.");

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void rejectsDuplicateEmailBeforeSaving() {
        Usuario newUser = usuario("iris", "iris@example.com", "password");
        when(usuarioRepository.existsByUsername("iris")).thenReturn(false);
        when(usuarioRepository.existsByEmail("iris@example.com")).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.crearUsuario(newUser))
                .isInstanceOf(UsuarioDuplicadoException.class)
                .hasMessage("El email ya se encuentra registrado.");

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void updateProfileCannotChangeOrClearStoredPassword() {
        Usuario current = usuario("iris", "iris@example.com", STORED_HASH);
        current.setId(7L);
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(current));
        when(usuarioRepository.existsByUsernameAndIdNot("iris_updated", 7L)).thenReturn(false);
        when(usuarioRepository.existsByEmailAndIdNot("updated@example.com", 7L)).thenReturn(false);
        when(usuarioRepository.save(current)).thenReturn(current);

        UsuarioUpdateRequest request = new UsuarioUpdateRequest(
                "iris_updated", "Iris", "De Dominicis", "updated@example.com", "ADMIN", true, null);
        Usuario updated = usuarioService.actualizarUsuario(7L, request);

        assertThat(updated.getPassword()).isEqualTo(STORED_HASH);
        assertThat(updated.getUsername()).isEqualTo("iris_updated");
    }

    @Test
    void rejectsDuplicateUsernameOnUpdateBeforeSaving() {
        Usuario current = usuario("iris", "iris@example.com", STORED_HASH);
        current.setId(7L);
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(current));
        when(usuarioRepository.existsByUsernameAndIdNot("taken", 7L)).thenReturn(true);

        UsuarioUpdateRequest request = new UsuarioUpdateRequest(
                "taken", "Iris", "De Dominicis", "iris@example.com", "ADMIN", true, null);

        assertThatThrownBy(() -> usuarioService.actualizarUsuario(7L, request))
                .isInstanceOf(UsuarioDuplicadoException.class);
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    private Usuario usuario(String username, String email, String password) {
        return new Usuario(username, "Iris", "De Dominicis", email, password, "ADMIN", true);
    }
}

package com.proyectohoussay.odonto.service;

import com.proyectohoussay.odonto.model.Usuario;
import com.proyectohoussay.odonto.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioService usuarioService;

    @Test
    void guardaContrasenaHasheadaYNormalizaIdentificadores() {
        Usuario usuario = new Usuario("Braian", "Aguilera", " Braian@Example.com ", "ADMINISTRADOR", true);
        usuario.setUsername(" Braian ");
        when(usuarioRepository.existsByEmailIgnoreCase("braian@example.com")).thenReturn(false);
        when(usuarioRepository.existsByUsernameIgnoreCase("braian")).thenReturn(false);
        when(passwordEncoder.encode("password-segura")).thenReturn("$2a$encoded");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Usuario creado = usuarioService.crearUsuario(usuario, "password-segura");

        assertThat(creado.getEmail()).isEqualTo("braian@example.com");
        assertThat(creado.getUsername()).isEqualTo("braian");
        assertThat(creado.getPasswordHash()).isEqualTo("$2a$encoded");
        verify(passwordEncoder).encode("password-segura");
    }

    @Test
    void rechazaEmailDuplicadoAntesDeGenerarElHash() {
        Usuario usuario = new Usuario("Braian", "Aguilera", "braian@example.com", "ADMINISTRADOR", true);
        when(usuarioRepository.existsByEmailIgnoreCase("braian@example.com")).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.crearUsuario(usuario, "password-segura"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("email ya se encuentra registrado");

        verifyNoInteractions(passwordEncoder);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void rechazaContrasenasEnBlancoODemasiadoLargas() {
        Usuario usuario = new Usuario("Braian", "Aguilera", "braian@example.com", "ADMINISTRADOR", true);

        assertThatThrownBy(() -> usuarioService.crearUsuario(usuario, " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("contraseña es obligatoria");

        assertThatThrownBy(() -> usuarioService.crearUsuario(usuario, "á".repeat(37)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("máximo permitido");

        verifyNoInteractions(passwordEncoder, usuarioRepository);
    }
}

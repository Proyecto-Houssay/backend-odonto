package com.proyectohoussay.odonto.auth;

import com.proyectohoussay.odonto.model.Usuario;
import com.proyectohoussay.odonto.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Test
    void autenticaPorNombreDeUsuario() {
        Usuario usuario = usuarioActivo("braian", "braian@example.com", "$2a$hash");
        when(usuarioRepository.findByUsernameIgnoreCase("braian")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("password-segura", "$2a$hash")).thenReturn(true);

        assertThat(authService.authenticate(" braian ", "password-segura")).isTrue();
        verify(usuarioRepository, never()).findByEmailIgnoreCase(anyString());
    }

    @Test
    void autenticaPorEmailSinDistinguirMayusculas() {
        Usuario usuario = usuarioActivo("braian", "braian@example.com", "$2a$hash");
        when(usuarioRepository.findByUsernameIgnoreCase("BRAIAN@EXAMPLE.COM")).thenReturn(Optional.empty());
        when(usuarioRepository.findByEmailIgnoreCase("BRAIAN@EXAMPLE.COM")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("password-segura", "$2a$hash")).thenReturn(true);

        assertThat(authService.authenticate("BRAIAN@EXAMPLE.COM", "password-segura")).isTrue();
    }

    @Test
    void rechazaUsuarioInexistenteYContrasenaIncorrecta() {
        when(usuarioRepository.findByUsernameIgnoreCase("desconocido")).thenReturn(Optional.empty());
        when(usuarioRepository.findByEmailIgnoreCase("desconocido")).thenReturn(Optional.empty());

        assertThat(authService.authenticate("desconocido", "password-segura")).isFalse();

        Usuario usuario = usuarioActivo("braian", "braian@example.com", "$2a$hash");
        when(usuarioRepository.findByUsernameIgnoreCase("braian")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("incorrecta", "$2a$hash")).thenReturn(false);

        assertThat(authService.authenticate("braian", "incorrecta")).isFalse();
    }

    @Test
    void rechazaUsuariosInactivosOSinHash() {
        Usuario inactivo = usuarioActivo("braian", "braian@example.com", "$2a$hash");
        inactivo.setActivo(false);
        when(usuarioRepository.findByUsernameIgnoreCase("braian")).thenReturn(Optional.of(inactivo));
        assertThat(authService.authenticate("braian", "password-segura")).isFalse();

        Usuario sinHash = usuarioActivo("iris", "iris@example.com", null);
        when(usuarioRepository.findByUsernameIgnoreCase("iris")).thenReturn(Optional.of(sinHash));
        assertThat(authService.authenticate("iris", "password-segura")).isFalse();
    }

    @Test
    void rechazaCredencialesVaciasYContrasenasDemasiadoLargas() {
        assertThat(authService.authenticate(" ", "password-segura")).isFalse();
        assertThat(authService.authenticate("braian", " ")).isFalse();
        assertThat(authService.authenticate("braian", "a".repeat(73))).isFalse();
        verifyNoInteractions(usuarioRepository, passwordEncoder);
    }

    private Usuario usuarioActivo(String username, String email, String passwordHash) {
        Usuario usuario = new Usuario("Braian", "Aguilera", email, "ADMINISTRADOR", true);
        usuario.setUsername(username);
        usuario.setPasswordHash(passwordHash);
        return usuario;
    }
}

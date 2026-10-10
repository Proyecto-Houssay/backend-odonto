package com.proyectohoussay.odonto.auth;

import com.proyectohoussay.odonto.model.Usuario;
import com.proyectohoussay.odonto.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private static final String RAW_PASSWORD = "clave-segura-para-prueba";
    private static final String USERNAME = "iris";

    private UsuarioRepository usuarioRepository;
    private PasswordEncoder passwordEncoder;
    private AuthService authService;
    private Usuario activeUser;

    @BeforeEach
    void setUp() {
        usuarioRepository = mock(UsuarioRepository.class);
        passwordEncoder = new BCryptPasswordEncoder();
        authService = new AuthService(usuarioRepository, passwordEncoder);
        activeUser = new Usuario(USERNAME, "Iris", "De Dominicis", "iris@example.com",
                passwordEncoder.encode(RAW_PASSWORD), "ADMINISTRADOR", true);
    }

    @Test
    void authenticatesActiveUserByUsernameUsingBCrypt() {
        when(usuarioRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(USERNAME, USERNAME)).thenReturn(Optional.of(activeUser));

        assertThat(authService.authenticate(USERNAME, RAW_PASSWORD)).contains(activeUser);
        verify(usuarioRepository).findByUsernameIgnoreCaseOrEmailIgnoreCase(USERNAME, USERNAME);
    }

    @Test
    void authenticatesActiveUserByEmail() {
        String email = "iris@example.com";
        when(usuarioRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(email, email)).thenReturn(Optional.of(activeUser));

        assertThat(authService.authenticate(email, RAW_PASSWORD)).contains(activeUser);
    }

    @Test
    void rejectsIncorrectPassword() {
        when(usuarioRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(USERNAME, USERNAME)).thenReturn(Optional.of(activeUser));

        assertThat(authService.authenticate(USERNAME, "incorrecta")).isEmpty();
    }

    @Test
    void rejectsUnknownUser() {
        when(usuarioRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase("desconocida", "desconocida"))
                .thenReturn(Optional.empty());

        assertThat(authService.authenticate("desconocida", RAW_PASSWORD)).isEmpty();
    }

    @Test
    void rejectsInactiveUserEvenWithCorrectPassword() {
        activeUser.setActivo(false);
        when(usuarioRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(USERNAME, USERNAME)).thenReturn(Optional.of(activeUser));

        assertThat(authService.authenticate(USERNAME, RAW_PASSWORD)).isEmpty();
    }

    @Test
    void rejectsBlankCredentialsWithoutQueryingRepository() {
        assertThat(authService.authenticate(" ", RAW_PASSWORD)).isEmpty();
        assertThat(authService.authenticate(USERNAME, " ")).isEmpty();
    }
}

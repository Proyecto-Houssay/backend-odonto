package com.proyectohoussay.odonto.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Test
    void respondeExitoCuandoLasCredencialesSonValidas() throws Exception {
        when(authService.authenticate("braian@example.com", "password-segura")).thenReturn(true);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"usernameOrEmail":"braian@example.com","password":"password-segura"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Inicio de sesión exitoso."));

        verify(authService).authenticate("braian@example.com", "password-segura");
    }

    @Test
    void devuelveErrorGenericoParaCredencialesIncorrectas() throws Exception {
        when(authService.authenticate("desconocido", "password-incorrecta")).thenReturn(false);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"usernameOrEmail":"desconocido","password":"password-incorrecta"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Credenciales incorrectas."))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("password-incorrecta"))));
    }

    @Test
    void validaQueUsuarioYContrasenaSeanObligatorios() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"usernameOrEmail":" ","password":" "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.allOf(
                                org.hamcrest.Matchers.containsString("usuario o correo electrónico"),
                                org.hamcrest.Matchers.containsString("contraseña"))));

        verifyNoInteractions(authService);
    }

    @Test
    void noReflejaUnaContrasenaExcesivaEnElError() throws Exception {
        String passwordLarga = "x".repeat(73);
        String request = "{\"usernameOrEmail\":\"braian\",\"password\":\"" + passwordLarga + "\"}";

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La contraseña supera el máximo permitido"))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString(passwordLarga))));

        verifyNoInteractions(authService);
    }

    @Test
    void devuelveMensajeControladoCuandoElJsonEsInvalido() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usernameOrEmail\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El cuerpo de la solicitud es inválido."));
    }
}

package com.proyectohoussay.odonto.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    private static final String PASSWORD = "secreto-de-prueba";
    private static final String HASH = "$2a$10$abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Test
    void loginSuccessfulReturnsOnlyConfirmationAndNeverCredentialsOrHash() throws Exception {
        given(authService.authenticate("iris", PASSWORD)).willReturn(true);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"usernameOrEmail":"iris","password":"%s"}
                                """.formatted(PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(content().string("Inicio de sesión exitoso"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(PASSWORD))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(HASH))));
    }

    @Test
    void wrongOrUnknownCredentialsUseSameSafeResponse() throws Exception {
        given(authService.authenticate("iris", PASSWORD)).willReturn(false);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"usernameOrEmail":"iris","password":"%s"}
                                """.formatted(PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Credenciales incorrectas"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(PASSWORD))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(HASH))));
    }

    @Test
    void missingCredentialHasClearBadRequestWithoutEchoingRequestBody() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"usernameOrEmail":" ","password":"%s"}
                                """.formatted(PASSWORD)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(PASSWORD))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(HASH))));
    }
}

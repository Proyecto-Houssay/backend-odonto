package com.proyectohoussay.odonto.auth;

import com.proyectohoussay.odonto.model.Usuario;
import com.proyectohoussay.odonto.repository.UsuarioRepository;
import com.proyectohoussay.odonto.repository.PasswordResetTokenRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PublicRegistrationIntegrationTest {

    private static final String PASSWORD = "demo-password-123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordResetTokenRepository resetTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private AccountEmailService emailService;

    private String resetUrl;

    @BeforeEach
    void clearUsers() {
        resetTokenRepository.deleteAll();
        usuarioRepository.deleteAll();
        resetUrl = null;
    }

    @Test
    void registersAReceptionistWithoutAllowingRoleEscalationAndCanLogIn() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"ana.nieto",
                                  "nombre":"Ana",
                                  "apellido":"Nieto",
                                  "email":"ANA.NIETO@example.com",
                                  "password":"%s",
                                  "rol":"ADMINISTRADOR",
                                  "activo":true
                                }
                                """.formatted(PASSWORD)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.emailSent").value(false))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(PASSWORD))));

        Usuario created = usuarioRepository.findByEmailIgnoreCase("ana.nieto@example.com").orElseThrow();
        assertThat(created.getRol()).isEqualTo("RECEPCIONISTA");
        assertThat(created.isActivo()).isTrue();
        assertThat(passwordEncoder.matches(PASSWORD, created.getPassword())).isTrue();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"usernameOrEmail":"ana.nieto@example.com","password":"%s"}
                                """.formatted(PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }

    @Test
    void rejectsDuplicateEmailAndUsername() throws Exception {
        String registration = """
                {"username":"ana","nombre":"Ana","apellido":"Nieto","email":"ana@example.com","password":"%s"}
                """.formatted(PASSWORD);
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(registration))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(registration))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void reportsRegistrationEmailAsSentOnlyWhenTheDeliveryServiceConfirmsIt() throws Exception {
        given(emailService.sendRegistrationConfirmation("ana@example.com", "Ana")).willReturn(true);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"ana\",\"nombre\":\"Ana\",\"apellido\":\"Nieto\",\"email\":\"ana@example.com\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.emailSent").value(true));
    }

    @Test
    void passwordResetUsesOneTimeTokenAndDoesNotRevealWhetherEmailExists() throws Exception {
        String registration = """
                {"username":"ana","nombre":"Ana","apellido":"Nieto","email":"ana@example.com","password":"%s"}
                """.formatted(PASSWORD);
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(registration))
                .andExpect(status().isCreated());

        given(emailService.isConfigured()).willReturn(true);
        doAnswer(invocation -> {
            resetUrl = invocation.getArgument(2);
            return true;
        }).when(emailService).sendPasswordReset(anyString(), anyString(), anyString());

        String knownEmailResponse = mockMvc.perform(post("/api/auth/password-reset/request")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"ana@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailConfigured").value(true))
                .andReturn().getResponse().getContentAsString();
        String unknownEmailResponse = mockMvc.perform(post("/api/auth/password-reset/request")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"missing@example.com\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(unknownEmailResponse).isEqualTo(knownEmailResponse);

        String rawToken = java.net.URI.create(resetUrl).getQuery().substring("token=".length());
        assertThat(resetTokenRepository.count()).isEqualTo(1);
        var storedToken = resetTokenRepository.findAll().get(0);
        assertThat(storedToken.getTokenHash()).isNotEqualTo(rawToken);
        assertThat(storedToken.getExpiresAt()).isAfter(Instant.now()).isBeforeOrEqualTo(Instant.now().plusSeconds(1800));

        mockMvc.perform(post("/api/auth/password-reset/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + rawToken + "\",\"newPassword\":\"brand-new-password\"}"))
                .andExpect(status().isNoContent());

        Usuario user = usuarioRepository.findByEmailIgnoreCase("ana@example.com").orElseThrow();
        assertThat(passwordEncoder.matches("brand-new-password", user.getPassword())).isTrue();
        assertThat(resetTokenRepository.count()).isZero();

        mockMvc.perform(post("/api/auth/password-reset/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + rawToken + "\",\"newPassword\":\"another-password\"}"))
                .andExpect(status().isBadRequest());
    }
}

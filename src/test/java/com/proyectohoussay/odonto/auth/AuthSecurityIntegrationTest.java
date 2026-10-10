package com.proyectohoussay.odonto.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyectohoussay.odonto.model.Usuario;
import com.proyectohoussay.odonto.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthSecurityIntegrationTest {

    private static final String PASSWORD = "test-password-123";
    private static final String JWT_SECRET_BASE64 = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        usuarioRepository.deleteAll();
        saveUser("admin", "admin@example.com", "ADMINISTRADOR", true);
        saveUser("dentist", "dentist@example.com", "ODONTOLOGO", true);
        saveUser("reception", "reception@example.com", "RECEPCIONISTA", true);
        saveUser("disabled", "disabled@example.com", "RECEPCIONISTA", false);
    }

    @Test
    void loginReturnsSignedShortLivedJwtWithUserIdAndNormalizedRole() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("dentist", PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        String token = response.get("token").asText();
        assertThat(token.split("\\.")).hasSize(3);
        JsonNode claims = objectMapper.readTree(Base64.getUrlDecoder().decode(token.split("\\.")[1]));
        assertThat(claims.get("sub").asText()).isEqualTo("dentist");
        assertThat(claims.get("userId").asLong()).isPositive();
        assertThat(claims.get("role").asText()).isEqualTo("ODONTOLOGO");

        mockMvc.perform(get("/api/turnos")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void invalidCredentialsAndInactiveUsersAreRejectedWithoutToken() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("dentist", "wrong-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.token").doesNotExist());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("disabled", PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    void healthAndLoginArePublicButOtherEndpointsRequireBearerToken() throws Exception {
        mockMvc.perform(get("/api/health")).andExpect(status().isOk());
        mockMvc.perform(get("/api/usuarios")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/usuarios")
                        .header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void expiredSignedJwtIsRejected() throws Exception {
        String token = signedToken("1", "ADMINISTRADOR", Instant.now().minusSeconds(1800), Instant.now().minusSeconds(900));

        mockMvc.perform(get("/api/usuarios")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void receptionistCanUseClinicalOperationsAndReadDirectoryButNotAdminOrClinicalRecords() throws Exception {
        String token = loginAndGetToken("reception");

        mockMvc.perform(get("/api/turnos").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/odontologos").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/especialidades").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/usuarios").header("Authorization", bearer(token)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/reports").header("Authorization", bearer(token)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/historias-clinicas").header("Authorization", bearer(token)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/usuarios")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void administratorCanManageUsersAndDentistCannotAccessAdministration() throws Exception {
        String administratorToken = loginAndGetToken("admin");
        mockMvc.perform(get("/api/usuarios").header("Authorization", bearer(administratorToken)))
                .andExpect(status().isOk());

        String dentistToken = loginAndGetToken("dentist");
        mockMvc.perform(get("/api/turnos").header("Authorization", bearer(dentistToken)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/usuarios").header("Authorization", bearer(dentistToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/reports").header("Authorization", bearer(dentistToken)))
                .andExpect(status().isForbidden());
    }

    private void saveUser(String username, String email, String role, boolean active) {
        Usuario user = new Usuario(username, username, "Test", email,
                passwordEncoder.encode(PASSWORD), role, active);
        usuarioRepository.save(user);
    }

    private String loginAndGetToken(String username) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(username, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    private String loginJson(String username, String password) {
        return """
                {"usernameOrEmail":"%s","password":"%s"}
                """.formatted(username, password);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String signedToken(String subject, String role, Instant issuedAt, Instant expiresAt) throws Exception {
        String header = Base64.getUrlEncoder().withoutPadding().encodeToString(
                "{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(("""
                {"sub":"%s","userId":1,"role":"%s","iat":%d,"exp":%d}
                """).formatted(subject, role, issuedAt.getEpochSecond(), expiresAt.getEpochSecond())
                .getBytes(StandardCharsets.UTF_8));
        String content = header + "." + payload;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(Base64.getDecoder().decode(JWT_SECRET_BASE64), "HmacSHA256"));
        String signature = Base64.getUrlEncoder().withoutPadding().encodeToString(
                mac.doFinal(content.getBytes(StandardCharsets.US_ASCII)));
        return content + "." + signature;
    }
}

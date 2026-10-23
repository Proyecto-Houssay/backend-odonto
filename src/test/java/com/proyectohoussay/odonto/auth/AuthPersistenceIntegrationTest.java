package com.proyectohoussay.odonto.auth;

import com.proyectohoussay.odonto.model.Usuario;
import com.proyectohoussay.odonto.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthPersistenceIntegrationTest {

    private static final String ADMIN_PASSWORD = "bootstrap-test-password";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void limpiarUsuarios() {
        usuarioRepository.deleteAll();
        usuarioRepository.save(new Usuario("admin", "Admin", "Test", "admin@example.com",
                passwordEncoder.encode(ADMIN_PASSWORD), "ADMINISTRADOR", true));
    }

    @Test
    void registraConHashYPermiteIngresarPorUsuarioOCorreo() throws Exception {
        mockMvc.perform(post("/api/usuarios")
                        .header("Authorization", "Bearer " + administratorToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre":"Ana",
                                  "apellido":"García",
                                  "email":"ANA@example.com",
                                  "username":"ana",
                                  "password":"clave123",
                                  "rol":"RECEPCIONISTA",
                                  "activo":true
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(content().string(not(containsString("clave123"))))
                .andExpect(content().string(not(containsString("passwordHash"))));

        Usuario usuario = usuarioRepository.findByEmail("ana@example.com").orElseThrow();
        assertThat(usuario.getPassword()).isNotEqualTo("clave123");
        assertThat(passwordEncoder.matches("clave123", usuario.getPassword())).isTrue();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"usernameOrEmail":"ana","password":"clave123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.token").exists());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"usernameOrEmail":"ANA@EXAMPLE.COM","password":"clave123"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"usernameOrEmail":"ana","password":"incorrecta"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Credenciales incorrectas."));
    }

    @Test
    void rechazaElAltaSinContrasena() throws Exception {
        mockMvc.perform(post("/api/usuarios")
                        .header("Authorization", "Bearer " + administratorToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre":"Ana",
                                  "apellido":"García",
                                  "email":"ana@example.com",
                                  "username":"ana",
                                  "rol":"RECEPCIONISTA"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    private String administratorToken() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"usernameOrEmail":"admin","password":"%s"}
                                """.formatted(ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.get("token").asText();
    }
}

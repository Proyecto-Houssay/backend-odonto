package com.proyectohoussay.odonto.auth;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthPersistenceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void limpiarUsuarios() {
        usuarioRepository.deleteAll();
    }

    @Test
    void registraConHashYPermiteIngresarPorUsuarioOCorreo() throws Exception {
        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre":"Ana",
                                  "apellido":"García",
                                  "email":"ANA@example.com",
                                  "username":"ana",
                                  "password":"clave123",
                                  "rol":"PACIENTE",
                                  "activo":true
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(content().string(not(containsString("clave123"))))
                .andExpect(content().string(not(containsString("passwordHash"))));

        Usuario usuario = usuarioRepository.findByEmailIgnoreCase("ana@example.com").orElseThrow();
        assertThat(usuario.getPasswordHash()).isNotEqualTo("clave123");
        assertThat(passwordEncoder.matches("clave123", usuario.getPasswordHash())).isTrue();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"usernameOrEmail":"ana","password":"clave123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Inicio de sesión exitoso."));

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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre":"Ana",
                                  "apellido":"García",
                                  "email":"ana@example.com",
                                  "username":"ana",
                                  "rol":"PACIENTE"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }
}

package com.proyectohoussay.odonto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyectohoussay.odonto.controller.UsuarioController;
import com.proyectohoussay.odonto.model.Usuario;
import com.proyectohoussay.odonto.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class UsuarioControllerTest {

    private static final String PASSWORD = "secreto-no-debe-responderse";

    private UsuarioService usuarioService;
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private Usuario usuario;

    @BeforeEach
    void preparar() {
        usuarioService = mock(UsuarioService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new UsuarioController(usuarioService)).build();
        objectMapper = new ObjectMapper();
        usuario = new Usuario("iris", "Iris", "De Dominicis", "iris@example.com", PASSWORD, "ADMIN", true);
        usuario.setId(7L);
    }

    @Test
    void listarUsuariosNoExponeLaContrasena() throws Exception {
        when(usuarioService.listarUsuarios()).thenReturn(List.of(usuario));

        mockMvc.perform(get("/api/usuarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].password").doesNotExist())
                .andExpect(content().string(not(containsString(PASSWORD))));
    }

    @Test
    void consultarUsuarioNoExponeLaContrasena() throws Exception {
        when(usuarioService.obtenerUsuario(7L)).thenReturn(usuario);

        mockMvc.perform(get("/api/usuarios/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(content().string(not(containsString(PASSWORD))));
    }

    @Test
    void crearUsuarioNoDevuelveLaContrasenaRecibida() throws Exception {
        when(usuarioService.crearUsuario(any(Usuario.class))).thenReturn(usuario);

        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("iris@example.com"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(content().string(not(containsString(PASSWORD))));
    }

    @Test
    void actualizarUsuarioNoDevuelveLaContrasena() throws Exception {
        when(usuarioService.actualizarUsuario(org.mockito.ArgumentMatchers.eq(7L), any(Usuario.class)))
                .thenReturn(usuario);

        mockMvc.perform(put("/api/usuarios/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("iris"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(content().string(not(containsString(PASSWORD))));
    }

    private Map<String, Object> requestBody() {
        return Map.of(
                "username", "iris",
                "nombre", "Iris",
                "apellido", "De Dominicis",
                "email", "iris@example.com",
                "password", PASSWORD,
                "rol", "ADMIN",
                "activo", true);
    }
}

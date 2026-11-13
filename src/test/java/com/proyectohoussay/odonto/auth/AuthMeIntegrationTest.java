package com.proyectohoussay.odonto.auth;

import com.proyectohoussay.odonto.model.CargoPersonal;
import com.proyectohoussay.odonto.model.Especialidad;
import com.proyectohoussay.odonto.model.Odontologo;
import com.proyectohoussay.odonto.model.Personal;
import com.proyectohoussay.odonto.model.Usuario;
import com.proyectohoussay.odonto.repository.EspecialidadRepository;
import com.proyectohoussay.odonto.repository.OdontologoRepository;
import com.proyectohoussay.odonto.repository.PersonalRepository;
import com.proyectohoussay.odonto.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("AuthMeIntegrationTest")
class AuthMeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private OdontologoRepository odontologoRepository;

    @Autowired
    private EspecialidadRepository especialidadRepository;

    @Autowired
    private PersonalRepository personalRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        personalRepository.deleteAll();
        odontologoRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    @Test
    @DisplayName("GET /api/auth/me resolves linked ODONTOLOGO profile")
    void getAuthMeResolvesOdontologoProfile() throws Exception {
        Usuario user = new Usuario(
                "dr_perez",
                "Juan",
                "Pérez",
                "perez@novadent.com",
                passwordEncoder.encode("clave123"),
                "ODONTOLOGO",
                true
        );
        user = usuarioRepository.save(user);

        Especialidad esp = especialidadRepository.findAll().stream().findFirst()
                .orElseGet(() -> especialidadRepository.save(new Especialidad("Ortodoncia", "Aparatos")));

        Odontologo o = new Odontologo("Juan", "Pérez", "MN-8844", "perez@novadent.com", "1122334455", esp);
        o.setUsuarioId(user.getId());
        odontologoRepository.save(o);

        // Login to get token
        String token = loginAndGetToken("dr_perez", "clave123");

        // Request /api/auth/me
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("dr_perez"))
                .andExpect(jsonPath("$.rol").value("ODONTOLOGO"))
                .andExpect(jsonPath("$.perfilVinculado.tipo").value("ODONTOLOGO"))
                .andExpect(jsonPath("$.perfilVinculado.matricula").value("MN-8844"));
    }

    @Test
    @DisplayName("GET /api/auth/me resolves linked PERSONAL profile for receptionist")
    void getAuthMeResolvesPersonalProfile() throws Exception {
        Usuario user = new Usuario(
                "recep_carlos",
                "Carlos",
                "Vega",
                "carlos@novadent.com",
                passwordEncoder.encode("clave123"),
                "RECEPCIONISTA",
                true
        );
        user = usuarioRepository.save(user);

        Personal p = new Personal(
                "Carlos",
                "Vega",
                "33221144",
                "carlos@novadent.com",
                "1155667788",
                CargoPersonal.RECEPCIONISTA,
                LocalDate.now(),
                "Turno Mañana",
                user.getId()
        );
        personalRepository.save(p);

        String token = loginAndGetToken("recep_carlos", "clave123");

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("recep_carlos"))
                .andExpect(jsonPath("$.rol").value("RECEPCIONISTA"))
                .andExpect(jsonPath("$.perfilVinculado.tipo").value("PERSONAL"))
                .andExpect(jsonPath("$.perfilVinculado.cargo").value("RECEPCIONISTA"));
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"usernameOrEmail": "%s", "password": "%s"}
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        int tokenStart = body.indexOf("\"token\":\"") + 9;
        int tokenEnd = body.indexOf("\"", tokenStart);
        return body.substring(tokenStart, tokenEnd);
    }
}

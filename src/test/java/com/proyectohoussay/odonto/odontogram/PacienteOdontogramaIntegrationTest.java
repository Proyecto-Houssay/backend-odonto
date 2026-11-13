package com.proyectohoussay.odonto.odontogram;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyectohoussay.odonto.auth.JwtTokenService;
import com.proyectohoussay.odonto.model.Usuario;
import com.proyectohoussay.odonto.patient.Paciente;
import com.proyectohoussay.odonto.patient.PacienteRepository;
import com.proyectohoussay.odonto.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.jpa.open-in-view=false",
        "spring.datasource.url=jdbc:h2:mem:patient-chart;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@Transactional
class PacienteOdontogramaIntegrationTest {
    private static final String PATH = "/api/portal-paciente/odontograma";
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired PacienteRepository patients;
    @Autowired UsuarioRepository users;
    @Autowired OdontogramaRepository charts;
    @Autowired JwtTokenService tokens;
    @Autowired JdbcTemplate jdbc;

    @Test
    void authenticationRoleAndPatientProfileAreRequired() throws Exception {
        mvc.perform(get(PATH)).andExpect(status().isUnauthorized());
        for (String role : List.of("RECEPCIONISTA", "ODONTOLOGO", "ADMINISTRADOR")) {
            mvc.perform(get(PATH).header("Authorization", token(account(role, null))))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(get(PATH).header("Authorization", token(account("PACIENTE", null))))
                .andExpect(status().isConflict());
        Usuario inactive = account("PACIENTE", patient());
        String inactiveToken = token(inactive);
        inactive.setActivo(false);
        users.saveAndFlush(inactive);
        mvc.perform(get(PATH).header("Authorization", inactiveToken)).andExpect(status().isForbidden());
    }

    @Test
    void missingChartIsExplicitlyEmptyRatherThanHealthy() throws Exception {
        Usuario owner = account("PACIENTE", patient());
        String body = mvc.perform(get(PATH).header("Authorization", token(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.piezas").isEmpty())
                .andReturn().getResponse().getContentAsString();
        JsonNode result = mapper.readTree(body);
        assertThat(result.has("registradoEn")).isTrue();
        assertThat(result.get("registradoEn").isNull()).isTrue();
        assertThat(result.size()).isEqualTo(2);
    }

    @Test
    void patientCanSeeOnlyLatestOwnMinimalChartEvenWhenAnotherIdIsSent() throws Exception {
        Paciente first = patient();
        Paciente second = patient();
        Usuario owner = account("PACIENTE", first);
        Usuario other = account("PACIENTE", second);
        Odontograma older = chart(first, 11, "caries", "firma-privada");
        Odontograma latest = chart(first, 12, "fractura", "otra-firma-privada");
        chart(second, 48, "ausente", "firma-ajena");
        // Equal timestamps exercise deterministic id-descending tie-breaking.
        jdbc.update("update odontograma set creado_en = ? where id in (?, ?)",
                java.sql.Timestamp.valueOf("2026-01-01 12:00:00"), older.response().id(), latest.response().id());

        String body = mvc.perform(get(PATH).param("pacienteId", second.getId().toString())
                        .header("Authorization", token(owner)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.registradoEn").isNotEmpty())
                .andExpect(jsonPath("$.piezas.length()").value(1))
                .andExpect(jsonPath("$.piezas[0].numero").value(12))
                .andExpect(jsonPath("$.piezas[0].afeccion").value("Fractura"))
                .andExpect(jsonPath("$.piezas[0].tratamiento").value("Reconstrucción con corona o carilla"))
                .andReturn().getResponse().getContentAsString();
        JsonNode result = mapper.readTree(body);
        assertThat(result.size()).isEqualTo(2);
        assertThat(result.path("piezas").get(0).size()).isEqualTo(3);
        assertThat(body).doesNotContain("firma", "consentimiento", "Presupuesto", "creadoPor", "\"numero\":48");
        mvc.perform(get(PATH).header("Authorization", token(other)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.piezas[0].numero").value(48));
        mvc.perform(get("/api/odontograma/catalogo").header("Authorization", token(owner)))
                .andExpect(status().isForbidden());
    }

    private Paciente patient() {
        Paciente patient = new Paciente();
        patient.setNombre("Demo"); patient.setApellido("Test");
        String id = UUID.randomUUID().toString();
        patient.setDni(id);
        patient.setEmail(id + "@example.com");
        patient.setFechaNacimiento(LocalDate.of(2000, 1, 1));
        return patients.saveAndFlush(patient);
    }

    private Usuario account(String role, Paciente patient) {
        String username = "chart-" + UUID.randomUUID();
        Usuario user = new Usuario(username, "Demo", "Test", username + "@example.com", "hash", role, true);
        user.setPaciente(patient);
        return users.saveAndFlush(user);
    }

    private String token(Usuario user) { return "Bearer " + tokens.issueToken(user).token(); }

    private Odontograma chart(Paciente patient, int number, String condition, String signature) {
        return charts.saveAndFlush(new Odontograma(patient, 123L,
                List.of(new OdontogramaDiente(number, OdontogramaCatalogo.buscar(condition))), signature));
    }
}

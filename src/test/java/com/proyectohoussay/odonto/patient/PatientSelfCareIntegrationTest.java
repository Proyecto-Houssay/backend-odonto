package com.proyectohoussay.odonto.patient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyectohoussay.odonto.auth.JwtTokenService;
import com.proyectohoussay.odonto.clinicalhistory.Diagnostico;
import com.proyectohoussay.odonto.clinicalhistory.EstadoTratamiento;
import com.proyectohoussay.odonto.clinicalhistory.HistoriaClinica;
import com.proyectohoussay.odonto.clinicalhistory.HistoriaClinicaRepository;
import com.proyectohoussay.odonto.clinicalhistory.Tratamiento;
import com.proyectohoussay.odonto.model.Especialidad;
import com.proyectohoussay.odonto.model.Odontologo;
import com.proyectohoussay.odonto.model.Turno;
import com.proyectohoussay.odonto.model.Usuario;
import com.proyectohoussay.odonto.repository.EspecialidadRepository;
import com.proyectohoussay.odonto.repository.OdontologoRepository;
import com.proyectohoussay.odonto.repository.TurnoRepository;
import com.proyectohoussay.odonto.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PatientSelfCareIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JwtTokenService tokens;
    @Autowired UsuarioRepository users;
    @Autowired PacienteRepository patients;
    @Autowired TurnoRepository turnos;
    @Autowired EspecialidadRepository especialidades;
    @Autowired OdontologoRepository odontologos;
    @Autowired HistoriaClinicaRepository historias;

    private String bearer(String username, String role) {
        Usuario user = users.saveAndFlush(new Usuario(username, "Demo", "Patient", username + "@example.com",
                "temporary-password", role, true));
        return "Bearer " + tokens.issueToken(user).token();
    }

    private Paciente createProfile(String token, String dni) throws Exception {
        String body = mvc.perform(post("/api/portal-paciente/perfil").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dni\":\"" + dni + "\",\"fechaNacimiento\":\"2004-08-11\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return patients.findById(json.readTree(body).get("id").asLong()).orElseThrow();
    }

    @Test
    void privateListsRequireAnActivePatientProfileAndStartEmpty() throws Exception {
        String patient = bearer("self-care-empty", "PACIENTE");
        String staff = bearer("self-care-staff", "RECEPCIONISTA");
        for (String path : new String[]{"/api/portal-paciente/turnos", "/api/portal-paciente/tratamientos"}) {
            mvc.perform(get(path)).andExpect(status().isUnauthorized());
            mvc.perform(get(path).header("Authorization", staff)).andExpect(status().isForbidden());
            mvc.perform(get(path).header("Authorization", patient)).andExpect(status().isConflict());
        }
        createProfile(patient, "self-care-001");
        mvc.perform(get("/api/portal-paciente/turnos").header("Authorization", patient))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
        mvc.perform(get("/api/portal-paciente/tratamientos").header("Authorization", patient))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void onlyOwnAppointmentsAreReturnedInDateTimeOrderWithoutPatientDetails() throws Exception {
        String first = bearer("self-care-appointments-1", "PACIENTE");
        String second = bearer("self-care-appointments-2", "PACIENTE");
        Paciente owner = createProfile(first, "self-care-002");
        Paciente other = createProfile(second, "self-care-003");
        Odontologo dentist = dentist();
        turnos.saveAndFlush(new Turno(LocalDate.of(2026, 11, 3), LocalTime.of(15, 0), "Control", "PENDIENTE", owner, dentist));
        turnos.saveAndFlush(new Turno(LocalDate.of(2026, 11, 2), LocalTime.of(9, 0), "Limpieza", "CONFIRMADO", owner, dentist));
        turnos.saveAndFlush(new Turno(LocalDate.of(2026, 11, 1), LocalTime.of(10, 0), "Privado", "CONFIRMADO", other, dentist));

        String body = mvc.perform(get("/api/portal-paciente/turnos").header("Authorization", first))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fecha").value("2026-11-02"))
                .andExpect(jsonPath("$[0].hora").value("09:00:00"))
                .andExpect(jsonPath("$[0].odontologoNombre").value("Ana Profesional"))
                .andExpect(jsonPath("$[0].paciente").doesNotExist())
                .andExpect(jsonPath("$[1].fecha").value("2026-11-03"))
                .andReturn().getResponse().getContentAsString();
        JsonNode list = json.readTree(body);
        assertThat(list).hasSize(2);
        assertThat(body).doesNotContain("Privado", other.getDni(), other.getEmail());
        mvc.perform(get("/api/portal-paciente/turnos").header("Authorization", second))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].motivo").value("Privado"));
    }

    @Test
    void onlyOwnTreatmentsAreReturnedNewestFirstWithoutAnotherHistory() throws Exception {
        String first = bearer("self-care-treatments-1", "PACIENTE");
        String second = bearer("self-care-treatments-2", "PACIENTE");
        Paciente owner = createProfile(first, "self-care-004");
        Paciente other = createProfile(second, "self-care-005");
        historyWithTreatment(owner.getId(), "Control antiguo", LocalDate.of(2026, 10, 1));
        historyWithTreatment(other.getId(), "Tratamiento ajeno", LocalDate.of(2026, 10, 15));
        HistoriaClinica history = historias.findByPacienteId(owner.getId()).orElseThrow();
        Diagnostico diagnosis = new Diagnostico(LocalDate.of(2026, 10, 20), "Evaluación");
        history.agregarDiagnostico(diagnosis);
        diagnosis.agregarTratamiento(new Tratamiento("Control reciente", LocalDate.of(2026, 10, 20), EstadoTratamiento.EN_CURSO));
        historias.saveAndFlush(history);

        String body = mvc.perform(get("/api/portal-paciente/tratamientos").header("Authorization", first))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].descripcion").value("Control reciente"))
                .andExpect(jsonPath("$[0].estado").value("EN_CURSO"))
                .andExpect(jsonPath("$[1].descripcion").value("Control antiguo"))
                .andExpect(jsonPath("$[0].diagnostico").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        assertThat(json.readTree(body)).hasSize(2);
        assertThat(body).doesNotContain("Tratamiento ajeno", other.getDni(), other.getEmail());
        mvc.perform(get("/api/portal-paciente/tratamientos").header("Authorization", second))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].descripcion").value("Tratamiento ajeno"));
    }

    private Odontologo dentist() {
        Especialidad specialty = especialidades.saveAndFlush(new Especialidad("General self-care", "General"));
        return odontologos.saveAndFlush(new Odontologo("Ana", "Profesional", "SELF-CARE-001", null, null, specialty));
    }

    private void historyWithTreatment(Long patientId, String description, LocalDate date) {
        HistoriaClinica history = new HistoriaClinica(date, patientId, null);
        Diagnostico diagnosis = new Diagnostico(date, "Evaluación");
        history.agregarDiagnostico(diagnosis);
        diagnosis.agregarTratamiento(new Tratamiento(description, date, EstadoTratamiento.PENDIENTE));
        historias.saveAndFlush(history);
    }
}

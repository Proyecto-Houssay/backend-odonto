package com.proyectohoussay.odonto.patient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyectohoussay.odonto.auth.JwtTokenService;
import com.proyectohoussay.odonto.model.Usuario;
import com.proyectohoussay.odonto.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PatientPortalIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UsuarioRepository users;
    @Autowired PacienteRepository patients;
    @Autowired JwtTokenService tokens;

    private String bearer(String username, String role) {
        Usuario user = users.saveAndFlush(new Usuario(username, "Demo", "Patient", username + "@example.com",
                "temporary-password", role, true));
        return "Bearer " + tokens.issueToken(user).token();
    }

    @Test
    void patientCreatesOnlyOwnProfileAndCannotReadStaffRoutes() throws Exception {
        String patient = bearer("portal-patient", "PACIENTE");
        mvc.perform(get("/api/pacientes").header("Authorization", patient)).andExpect(status().isForbidden());
        mvc.perform(get("/api/usuarios").header("Authorization", patient)).andExpect(status().isForbidden());
        mvc.perform(get("/api/odontograma/catalogo").header("Authorization", patient)).andExpect(status().isForbidden());
        mvc.perform(get("/api/portal-paciente/perfil").header("Authorization", patient)).andExpect(status().isNotFound());

        mvc.perform(post("/api/portal-paciente/perfil").header("Authorization", patient)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"dni":"44555111","telefono":"1100000000","fechaNacimiento":"2004-08-11"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Demo"))
                .andExpect(jsonPath("$.dni").value("44555111"));
        mvc.perform(get("/api/portal-paciente/perfil").header("Authorization", patient))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value("portal-patient@example.com"));
        mvc.perform(put("/api/portal-paciente/perfil").header("Authorization", patient)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"telefono\":\"1199999999\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.telefono").value("1199999999"));
        mvc.perform(post("/api/portal-paciente/perfil").header("Authorization", patient)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dni\":\"44555112\",\"fechaNacimiento\":\"2004-08-11\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void reportTimelineIsPrivateAndDentistCanReviewWithoutAutomaticDiagnosis() throws Exception {
        String patient = bearer("portal-reporter", "PACIENTE");
        String other = bearer("portal-other", "PACIENTE");
        String dentist = bearer("portal-dentist", "ODONTOLOGO");
        String reception = bearer("portal-reception", "RECEPCIONISTA");
        mvc.perform(post("/api/portal-paciente/perfil").header("Authorization", patient)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dni\":\"44555113\",\"fechaNacimiento\":\"2004-08-11\"}"))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/portal-paciente/perfil").header("Authorization", other)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dni\":\"44555115\",\"fechaNacimiento\":\"2004-08-11\"}"))
                .andExpect(status().isCreated());

        String first = mvc.perform(post("/api/portal-paciente/reportes").header("Authorization", patient)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pieza\":11,\"dolor\":6,\"sintomas\":\"Molestia al masticar\",\"fotoBase64\":null}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.revisionProfesional").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        long reportId = json.readTree(first).get("id").asLong();
        mvc.perform(post("/api/portal-paciente/reportes").header("Authorization", patient)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pieza\":11,\"dolor\":4,\"sintomas\":\"Nueva observación\",\"fotoBase64\":null}"))
                .andExpect(status().isCreated());
        JsonNode timeline = json.readTree(mvc.perform(get("/api/portal-paciente/reportes").header("Authorization", patient))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(timeline).hasSize(2);

        mvc.perform(get("/api/portal-paciente/reportes/" + reportId).header("Authorization", other))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/seguimiento/reportes/" + reportId).header("Authorization", reception))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/seguimiento/reportes/" + reportId + "/revision")
                        .header("Authorization", patient).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"revisionProfesional\":\"Evaluar en consulta.\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/seguimiento/reportes/" + reportId + "/revision")
                        .header("Authorization", dentist).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"revisionProfesional\":\"Evaluar en consulta.\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("REVISADO"));
        mvc.perform(put("/api/seguimiento/reportes/" + reportId + "/revision")
                        .header("Authorization", dentist).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"revisionProfesional\":\"Otra nota.\"}"))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/portal-paciente/reportes/" + reportId).header("Authorization", patient))
                .andExpect(status().isOk()).andExpect(jsonPath("$.revisionProfesional").value("Evaluar en consulta."));
    }

    @Test
    void administratorLinksExistingPatientWithoutGuessingOwnershipFromDni() throws Exception {
        String patientToken = bearer("portal-linked", "PACIENTE");
        Usuario account = users.findByUsername("portal-linked").orElseThrow();
        String admin = bearer("portal-admin", "ADMINISTRADOR");
        Paciente existing = new Paciente();
        existing.setNombre("Existing"); existing.setApellido("Patient"); existing.setDni("44555114");
        existing.setEmail("existing@example.com"); existing.setFechaNacimiento(java.time.LocalDate.of(2000, 1, 1));
        existing = patients.saveAndFlush(existing);
        mvc.perform(put("/api/admin/vinculos-paciente/" + account.getId())
                        .header("Authorization", patientToken).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pacienteId\":" + existing.getId() + "}"))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/admin/vinculos-paciente/" + account.getId())
                        .header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pacienteId\":" + existing.getId() + ",\"dniConfirmado\":\"incorrecto\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/admin/vinculos-paciente/" + account.getId())
                        .header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pacienteId\":" + existing.getId() + ",\"dniConfirmado\":\"44555114\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(existing.getId()));
        mvc.perform(get("/api/portal-paciente/perfil").header("Authorization", patientToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.dni").value("44555114"));
    }

    @Test
    void photoRequiresExplicitConsentAndIsReturnedOnlyToOwnerOrClinician() throws Exception {
        String patient = bearer("portal-photo", "PACIENTE");
        String other = bearer("portal-photo-other", "PACIENTE");
        String dentist = bearer("portal-photo-dentist", "ODONTOLOGO");
        mvc.perform(post("/api/portal-paciente/perfil").header("Authorization", patient)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dni\":\"44555116\",\"fechaNacimiento\":\"2004-08-11\"}"))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/portal-paciente/perfil").header("Authorization", other)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dni\":\"44555117\",\"fechaNacimiento\":\"2004-08-11\"}"))
                .andExpect(status().isCreated());
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", output);
        String photo = "data:image/png;base64," + Base64.getEncoder().encodeToString(output.toByteArray());
        String body = "{\"pieza\":11,\"dolor\":2,\"sintomas\":\"Observación del paciente\",\"fotoBase64\":\"" + photo + "\"}";
        mvc.perform(post("/api/portal-paciente/reportes").header("Authorization", patient)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        String created = mvc.perform(post("/api/portal-paciente/reportes").header("Authorization", patient)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body.substring(0, body.length() - 1) + ",\"consentimientoFoto\":true}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fotoBase64").value(org.hamcrest.Matchers.startsWith("data:image/png;base64,")))
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(created).get("id").asLong();
        mvc.perform(get("/api/portal-paciente/reportes/" + id).header("Authorization", other))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/seguimiento/reportes/" + id).header("Authorization", dentist))
                .andExpect(status().isOk()).andExpect(jsonPath("$.fotoBase64").exists());
    }
}

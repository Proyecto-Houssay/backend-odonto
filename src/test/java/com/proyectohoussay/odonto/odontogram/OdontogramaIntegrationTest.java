package com.proyectohoussay.odonto.odontogram;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyectohoussay.odonto.auth.JwtTokenService;
import com.proyectohoussay.odonto.model.Usuario;
import com.proyectohoussay.odonto.patient.Paciente;
import com.proyectohoussay.odonto.patient.PacienteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"spring.jpa.open-in-view=false", "spring.datasource.url=jdbc:h2:mem:odontograma;DB_CLOSE_DELAY=-1"})
@AutoConfigureMockMvc
class OdontogramaIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired PacienteRepository patients;
    @Autowired JwtTokenService tokens;
    @Autowired JdbcTemplate jdbc;
    long patientId;
    String dentist;
    String admin;
    String reception;
    String png;

    @BeforeEach
    void setup() throws Exception {
        Paciente patient = new Paciente();
        patient.setNombre("Prueba"); patient.setApellido("Académica");
        patient.setDni(UUID.randomUUID().toString()); patient.setEmail("test@example.com");
        patient.setFechaNacimiento(LocalDate.of(1990, 1, 1));
        patientId = patients.saveAndFlush(patient).getId();
        dentist = token("ODONTOLOGO"); admin = token("ADMINISTRADOR"); reception = token("RECEPCIONISTA");
        png = png(500, 180);
    }

    @Test
    void persistsCanonicalImmutableSnapshotAndClosedSessionDtos() throws Exception {
        Map<String, Object> request = request();
        request.put("totalPresupuesto", 1); request.put("creadoPorId", 999);
        request.put("dientes", List.of(Map.of("numero", 11, "afeccion", "caries", "costoEstimado", 1),
                Map.of("numero", 48, "afeccion", "sano")));
        MvcResult result = create(request).andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.totalPresupuesto").value(45000))
                .andExpect(jsonPath("$.creadoPorId").value(73))
                .andExpect(jsonPath("$.moneda").value("ARS"))
                .andExpect(jsonPath("$.versionCatalogo").value("academic-v1"))
                .andExpect(jsonPath("$.versionConsentimiento").value("academic-v1"))
                .andExpect(jsonPath("$.consentimientoAceptado").value(true))
                .andExpect(jsonPath("$.dientes[0].tratamiento").value("Limpieza + empaste con resina compuesta"))
                .andExpect(jsonPath("$.dientes[0].costoEstimado").value(45000))
                .andExpect(jsonPath("$.dientes[0].color").value("#ef4444")).andReturn();
        JsonNode saved = mapper.readTree(result.getResponse().getContentAsString());
        long id = saved.get("id").asLong();
        mvc.perform(get("/api/odontograma/" + id).header("Authorization", dentist))
                .andExpect(status().isOk()).andExpect(content().json(saved.toString()));
        mvc.perform(get("/api/odontograma").param("pacienteId", "" + patientId).header("Authorization", admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(id))
                .andExpect(jsonPath("$[0].firmaBase64").doesNotExist())
                .andExpect(jsonPath("$[0].totalPresupuesto").value(45000));
        assertThat(jdbc.queryForObject("select total_presupuesto from odontograma where id=?", Integer.class, id)).isEqualTo(45000);
        assertThat(jdbc.queryForObject("select count(*) from odontograma_diente where odontograma_id=?", Integer.class, id)).isEqualTo(2);
        mvc.perform(delete("/api/pacientes/" + patientId).header("Authorization", admin)).andExpect(status().isConflict());
        assertThat(patients.existsById(patientId)).isTrue();
        mvc.perform(put("/api/odontograma/" + id).header("Authorization", dentist)
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(request)))
                .andExpect(status().isMethodNotAllowed());
        mvc.perform(delete("/api/odontograma/" + id).header("Authorization", dentist)).andExpect(status().isMethodNotAllowed());
    }

    @Test
    void authorizesEveryEndpointForClinicalRolesOnly() throws Exception {
        for (String path : List.of("/api/odontograma?pacienteId=" + patientId, "/api/odontograma/catalogo", "/api/odontograma/999999")) {
            mvc.perform(get(path)).andExpect(status().isUnauthorized());
            mvc.perform(get(path).header("Authorization", reception)).andExpect(status().isForbidden());
        }
        mvc.perform(post("/api/odontograma").contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(request())))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/odontograma").header("Authorization", reception).contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request()))).andExpect(status().isForbidden());
        create(request()).andExpect(status().isCreated());
        mvc.perform(post("/api/odontograma").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request()))).andExpect(status().isCreated());
    }

    @Test
    void catalogHasExactCodesPricesAndConsent() throws Exception {
        MvcResult result = mvc.perform(get("/api/odontograma/catalogo").header("Authorization", dentist))
                .andExpect(status().isOk()).andExpect(jsonPath("$.versionCatalogo").value("academic-v1"))
                .andExpect(jsonPath("$.versionConsentimiento").value("academic-v1")).andReturn();
        JsonNode catalog = mapper.readTree(result.getResponse().getContentAsString());
        assertThat(catalog.get("afecciones").size()).isEqualTo(5);
        assertThat(catalog.get("textoConsentimiento").asText()).contains("simulación académica", "validación profesional");
        int[] costs = {45000, 85000, 30000, 250000, 0};
        String[] codes = {"caries", "fractura", "gingivitis", "ausente", "sano"};
        String[] colors = {"#ef4444", "#f97316", "#eab308", "#6b7280", "#22c55e"};
        for (int i = 0; i < 5; i++) {
            assertThat(catalog.get("afecciones").get(i).get("afeccion").asText()).isEqualTo(codes[i]);
            assertThat(catalog.get("afecciones").get(i).get("costoEstimado").asInt()).isEqualTo(costs[i]);
            assertThat(catalog.get("afecciones").get(i).get("color").asText()).isEqualTo(colors[i]);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "9", "10", "19", "29", "39", "49", "51", "-11", "99"})
    void rejectsInvalidFdiAtomically(String number) throws Exception {
        Map<String, Object> input = request(); input.put("dientes", List.of(Map.of("numero", Integer.parseInt(number), "afeccion", "caries")));
        assertRejected(input);
    }

    @Test
    void acceptsAllThirtyTwoFdiAndComputesIntegerTotal() throws Exception {
        var teeth = new java.util.ArrayList<Map<String, Object>>();
        for (int q = 1; q <= 4; q++) for (int t = 1; t <= 8; t++) teeth.add(Map.of("numero", q * 10 + t, "afeccion", "caries"));
        var input = request(); input.put("dientes", teeth);
        create(input).andExpect(status().isCreated()).andExpect(jsonPath("$.dientes.length()").value(32))
                .andExpect(jsonPath("$.totalPresupuesto").value(1440000));
        teeth.add(Map.of("numero", 11, "afeccion", "sano")); assertRejected(input);
    }

    @Test
    void rejectsDuplicateUnknownEmptyAndMissingFields() throws Exception {
        for (Object teeth : List.of(List.of(), List.of(Map.of("numero", 11, "afeccion", "sano"), Map.of("numero", 11, "afeccion", "caries")),
                List.of(Map.of("numero", 11, "afeccion", "Caries")), List.of(Map.of("numero", 11, "afeccion", "inventado")),
                List.of(Map.of("numero", 11)), List.of(Map.of("afeccion", "sano")))) {
            var input = request(); input.put("dientes", teeth); assertRejected(input);
        }
        for (String key : List.of("pacienteId", "dientes", "consentimientoAceptado", "versionConsentimiento", "firmaBase64")) {
            var input = request(); input.remove(key); assertRejected(input);
        }
    }

    @Test
    void rejectsMissingPatientInvalidConsentAndVersion() throws Exception {
        var input = request(); input.put("pacienteId", 999999999);
        create(input).andExpect(status().isNotFound());
        input = request(); input.put("pacienteId", 0); assertRejected(input);
        input = request(); input.put("consentimientoAceptado", false); assertRejected(input);
        input = request(); input.put("versionConsentimiento", "old"); assertRejected(input);
        mvc.perform(get("/api/odontograma/999999999").header("Authorization", dentist)).andExpect(status().isNotFound());
        mvc.perform(get("/api/odontograma").param("pacienteId", "0").header("Authorization", dentist)).andExpect(status().isBadRequest());
    }

    @Test
    void rejectsMalformedOversizedNonPngAndCorruptImagesAtomically() throws Exception {
        byte[] hugeHeader = Base64.getDecoder().decode(png.substring(png.indexOf(',') + 1));
        hugeHeader[16] = 0x7f; hugeHeader[17] = (byte) 0xff; hugeHeader[18] = (byte) 0xff; hugeHeader[19] = (byte) 0xff;
        String corrupt = "data:image/png;base64," + Base64.getEncoder().encodeToString(java.util.Arrays.copyOf(hugeHeader, 35));
        for (String value : List.of("", "data:image/png;base64,%%%", "data:image/jpeg;base64,AAAA", "data:image/png;base64,AAAA",
                "data:image/png;base64," + "A".repeat(350000), "data:image/png;base64," + Base64.getEncoder().encodeToString(hugeHeader), corrupt,
                png(2049, 1), png(2000, 2000))) {
            var input = request(); input.put("firmaBase64", value); assertRejected(input);
        }
    }

    void assertRejected(Map<String, Object> input) throws Exception {
        int before = jdbc.queryForObject("select count(*) from odontograma", Integer.class);
        create(input).andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("select count(*) from odontograma", Integer.class)).isEqualTo(before);
    }

    org.springframework.test.web.servlet.ResultActions create(Map<String, Object> input) throws Exception {
        return mvc.perform(post("/api/odontograma").header("Authorization", dentist).contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(input)));
    }
    Map<String, Object> request() {
        var input = new LinkedHashMap<String, Object>();
        input.put("pacienteId", patientId); input.put("dientes", List.of(Map.of("numero", 11, "afeccion", "caries")));
        input.put("consentimientoAceptado", true); input.put("versionConsentimiento", "academic-v1"); input.put("firmaBase64", png);
        return input;
    }
    String token(String role) {
        Usuario user = new Usuario("odontogram-test", "Test", "User", "test@example.com", "unused", role, true);
        user.setId(73L);
        return "Bearer " + tokens.issueToken(user).token();
    }
    static String png(int width, int height) throws Exception {
        var image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, 0xff000000);
        var bytes = new ByteArrayOutputStream(); ImageIO.write(image, "png", bytes);
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes.toByteArray());
    }
}

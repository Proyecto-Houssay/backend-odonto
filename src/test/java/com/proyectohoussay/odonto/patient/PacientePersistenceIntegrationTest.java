package com.proyectohoussay.odonto.patient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class PacientePersistenceIntegrationTest {

    private static final String VALID_PATIENT_REQUEST = """
            {
              "nombre": "Ana",
              "apellido": "Pérez",
              "dni": "12345678",
              "telefono": "1123456789",
              "email": "ana@example.com",
              "fechaNacimiento": "1990-01-01"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PacienteRepository pacienteRepository;

    @BeforeEach
    void cleanPatientsBeforeTest() {
        pacienteRepository.deleteAll();
    }

    @AfterEach
    void cleanPatientsAfterTest() {
        pacienteRepository.deleteAll();
    }

    @Test
    void postPersistsPatientFieldsAndGeneratedId() throws Exception {
        mockMvc.perform(post("/api/pacientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PATIENT_REQUEST))
                .andExpect(status().isOk())
                .andExpect(content().string("Paciente registrado correctamente"));

        List<Paciente> persistedPatients = pacienteRepository.findAll();

        assertThat(persistedPatients).hasSize(1);
        Paciente persistedPatient = persistedPatients.get(0);
        assertThat(persistedPatient.getId()).isNotNull();
        assertThat(persistedPatient.getNombre()).isEqualTo("Ana");
        assertThat(persistedPatient.getApellido()).isEqualTo("Pérez");
        assertThat(persistedPatient.getDni()).isEqualTo("12345678");
        assertThat(persistedPatient.getTelefono()).isEqualTo("1123456789");
        assertThat(persistedPatient.getEmail()).isEqualTo("ana@example.com");
        assertThat(persistedPatient.getFechaNacimiento()).isEqualTo(LocalDate.of(1990, 1, 1));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidPatientRequests")
    void invalidPostReturnsBadRequestWithoutPersistingPatient(
            String scenario, String invalidRequest, String expectedMessage) throws Exception {
        mockMvc.perform(post("/api/pacientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(expectedMessage));

        assertThat(pacienteRepository.count()).isZero();
    }

    private static Stream<Arguments> invalidPatientRequests() {
        return Stream.of(
                Arguments.of(
                        "blank name",
                        VALID_PATIENT_REQUEST.replace("\"nombre\": \"Ana\"", "\"nombre\": \"\""),
                        "El nombre es obligatorio"),
                Arguments.of(
                        "blank surname",
                        VALID_PATIENT_REQUEST.replace("\"apellido\": \"Pérez\"", "\"apellido\": \"\""),
                        "El apellido es obligatorio"),
                Arguments.of(
                        "blank DNI",
                        VALID_PATIENT_REQUEST.replace("\"dni\": \"12345678\"", "\"dni\": \"\""),
                        "El DNI es obligatorio"),
                Arguments.of(
                        "blank email",
                        VALID_PATIENT_REQUEST.replace("\"email\": \"ana@example.com\"", "\"email\": \"\""),
                        "El correo electrónico es obligatorio"),
                Arguments.of(
                        "missing birth date",
                        VALID_PATIENT_REQUEST.replace(
                                ",\n  \"fechaNacimiento\": \"1990-01-01\"", ""),
                        "La fecha de nacimiento es obligatoria"),
                Arguments.of(
                        "malformed email",
                        VALID_PATIENT_REQUEST.replace(
                                "\"email\": \"ana@example.com\"", "\"email\": \"not-an-email\""),
                        "El correo electrónico no tiene un formato válido"),
                Arguments.of(
                        "future birth date",
                        VALID_PATIENT_REQUEST.replace(
                                "\"fechaNacimiento\": \"1990-01-01\"",
                                "\"fechaNacimiento\": \"" + LocalDate.now().plusDays(1) + "\""),
                        "La fecha de nacimiento no puede ser futura"));
    }

    @Test
    void postAcceptsBirthDateToday() throws Exception {
        String request = VALID_PATIENT_REQUEST.replace(
                "\"fechaNacimiento\": \"1990-01-01\"",
                "\"fechaNacimiento\": \"" + LocalDate.now() + "\"");

        mockMvc.perform(post("/api/pacientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(content().string("Paciente registrado correctamente"));

        assertThat(pacienteRepository.count()).isEqualTo(1);
    }

    @Test
    void duplicateDniPostReturnsConflictWithoutCreatingAnotherRow() throws Exception {
        mockMvc.perform(post("/api/pacientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PATIENT_REQUEST))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/pacientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PATIENT_REQUEST))
                .andExpect(status().isConflict())
                .andExpect(content().string("Ya existe un paciente registrado con ese DNI"));

        assertThat(pacienteRepository.count()).isEqualTo(1);
    }
}

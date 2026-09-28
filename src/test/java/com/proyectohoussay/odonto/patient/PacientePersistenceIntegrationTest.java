package com.proyectohoussay.odonto.patient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
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

    @Test
    void invalidPostReturnsBadRequestWithoutPersistingPatient() throws Exception {
        String invalidRequest = VALID_PATIENT_REQUEST.replace("\"nombre\": \"Ana\"", "\"nombre\": \"\"");

        mockMvc.perform(post("/api/pacientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("El nombre es obligatorio"));

        assertThat(pacienteRepository.count()).isZero();
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

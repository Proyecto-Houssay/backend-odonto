package com.proyectohoussay.odonto.clinicalhistory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyectohoussay.odonto.patient.Paciente;
import com.proyectohoussay.odonto.patient.PacienteRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class ClinicalRecordPersistenceIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired PacienteRepository pacienteRepository;
    @Autowired HistoriaClinicaRepository historiaRepository;
    @Autowired DiagnosticoRepository diagnosticoRepository;
    @Autowired TratamientoRepository tratamientoRepository;

    @Test
    void swaggerStylePostChainPersistsToDatabase() throws Exception {
        Paciente patient = new Paciente();
        patient.setNombre("Ana");
        patient.setApellido("Prueba");
        patient.setDni("clinical-chain-123");
        patient.setEmail("clinical-chain@example.com");
        patient.setFechaNacimiento(LocalDate.of(1990, 1, 1));
        Long patientId = pacienteRepository.save(patient).getId();

        var historyResult = mockMvc.perform(post("/api/historias-clinicas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pacienteId\":" + patientId + ",\"fechaApertura\":\"2026-10-14\",\"observaciones\":\"Control\"}"))
                .andExpect(status().isCreated()).andReturn();
        Long historyId = objectMapper.readTree(historyResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(delete("/api/pacientes/" + patientId))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/historias-clinicas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pacienteId\":" + patientId + ",\"fechaApertura\":\"2026-10-14\"}"))
                .andExpect(status().isConflict());
        mockMvc.perform(get("/api/historias-clinicas").param("pacienteId", patientId.toString()))
                .andExpect(status().isOk());

        var diagnosisResult = mockMvc.perform(post("/api/diagnosticos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"historiaClinicaId\":" + historyId + ",\"fecha\":\"2026-10-14\",\"descripcion\":\"Caries\"}"))
                .andExpect(status().isCreated()).andReturn();
        Long diagnosisId = objectMapper.readTree(diagnosisResult.getResponse().getContentAsString()).get("id").asLong();

        var treatmentResult = mockMvc.perform(post("/api/tratamientos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"diagnosticoId\":" + diagnosisId + ",\"descripcion\":\"Obturación\",\"fecha\":\"2026-10-14\",\"estado\":\"PENDIENTE\"}"))
                .andExpect(status().isCreated()).andReturn();
        Long treatmentId = objectMapper.readTree(treatmentResult.getResponse().getContentAsString()).get("id").asLong();

        assertThat(historiaRepository.findByPacienteId(patientId)).isPresent();
        assertThat(diagnosticoRepository.findAllByHistoriaClinicaId(historyId)).hasSize(1);
        assertThat(tratamientoRepository.findAllByDiagnosticoId(diagnosisId)).hasSize(1);
        assertThat(tratamientoRepository.findById(treatmentId).orElseThrow().getEstado())
                .isEqualTo(EstadoTratamiento.PENDIENTE);
    }
}

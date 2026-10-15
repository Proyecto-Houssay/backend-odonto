package com.proyectohoussay.odonto.clinicalhistory;

import com.proyectohoussay.odonto.patient.Paciente;
import com.proyectohoussay.odonto.patient.PacienteRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.jpa.open-in-view=false",
        "spring.datasource.url=jdbc:h2:mem:clinical-no-oiv;DB_CLOSE_DELAY=-1"
})
@AutoConfigureMockMvc(addFilters = false)
class ClinicalRecordClosedSessionIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired PacienteRepository pacienteRepository;
    @Autowired HistoriaClinicaRepository historiaRepository;
    @Autowired DiagnosticoRepository diagnosticoRepository;
    @Autowired TratamientoRepository tratamientoRepository;

    @Test
    void getAndPutUseFlatDtosAfterRepositorySessionCloses() throws Exception {
        Paciente patient = new Paciente();
        patient.setNombre("Ana");
        patient.setApellido("Prueba");
        patient.setDni("closed-session-123");
        patient.setEmail("closed-session@example.com");
        patient.setFechaNacimiento(LocalDate.of(1990, 1, 1));
        Long patientId = pacienteRepository.saveAndFlush(patient).getId();
        HistoriaClinica history = historiaRepository.saveAndFlush(
                new HistoriaClinica(LocalDate.of(2026, 10, 14), patientId, "Control"));
        Diagnostico diagnosis = new Diagnostico(LocalDate.of(2026, 10, 14), "Caries");
        history.agregarDiagnostico(diagnosis);
        diagnosis = diagnosticoRepository.saveAndFlush(diagnosis);
        Tratamiento treatment = new Tratamiento("Obturación", LocalDate.of(2026, 10, 14), EstadoTratamiento.PENDIENTE);
        diagnosis.agregarTratamiento(treatment);
        treatment = tratamientoRepository.saveAndFlush(treatment);

        String historyPath = "/api/historias-clinicas/" + history.getId();
        String diagnosisPath = "/api/diagnosticos/" + diagnosis.getId();
        String treatmentPath = "/api/tratamientos/" + treatment.getId();

        mockMvc.perform(get(historyPath)).andExpect(status().isOk())
                .andExpect(jsonPath("$.pacienteId").value(patientId));
        mockMvc.perform(get(diagnosisPath)).andExpect(status().isOk())
                .andExpect(jsonPath("$.historiaClinicaId").value(history.getId()));
        mockMvc.perform(get(treatmentPath)).andExpect(status().isOk())
                .andExpect(jsonPath("$.diagnosticoId").value(diagnosis.getId()));

        mockMvc.perform(put(historyPath).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fechaApertura\":\"2026-10-15\",\"observaciones\":\"Revisión\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.observaciones").value("Revisión"));
        mockMvc.perform(put(diagnosisPath).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fecha\":\"2026-10-15\",\"descripcion\":\"Gingivitis\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.historiaClinicaId").value(history.getId()));
        mockMvc.perform(put(treatmentPath).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"descripcion\":\"Limpieza\",\"fecha\":\"2026-10-15\",\"estado\":\"COMPLETADO\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("COMPLETADO"));
    }
}

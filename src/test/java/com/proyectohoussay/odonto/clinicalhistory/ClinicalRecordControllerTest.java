package com.proyectohoussay.odonto.clinicalhistory;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClinicalRecordController.class)
@AutoConfigureMockMvc(addFilters = false)
class ClinicalRecordControllerTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean ClinicalRecordService service;

    @Test
    void createsHistoryAsSafeDto() throws Exception {
        HistoriaClinica saved = new HistoriaClinica(LocalDate.of(2026, 10, 14), 7L, "Control");
        when(service.createHistory(7L, LocalDate.of(2026, 10, 14), "Control")).thenReturn(saved);

        mockMvc.perform(post("/api/historias-clinicas").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pacienteId":7,"fechaApertura":"2026-10-14","observaciones":"Control"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pacienteId").value(7))
                .andExpect(jsonPath("$.diagnosticos").doesNotExist());
    }

    @Test
    void validatesDiagnosisBeforeService() throws Exception {
        mockMvc.perform(post("/api/diagnosticos").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"historiaClinicaId":3,"fecha":"2026-10-14","descripcion":""}
                                """))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void unknownHistoryIsNotFound() throws Exception {
        when(service.getHistory(999L)).thenThrow(new ClinicalRecordNotFoundException("Historia clínica", 999L));
        mockMvc.perform(get("/api/historias-clinicas/999")).andExpect(status().isNotFound());
    }

    @Test
    void invalidTreatmentStateIsBadRequest() throws Exception {
        mockMvc.perform(post("/api/tratamientos").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"diagnosticoId":3,"descripcion":"Control","fecha":"2026-10-14","estado":"DESCONOCIDO"}
                                """))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}

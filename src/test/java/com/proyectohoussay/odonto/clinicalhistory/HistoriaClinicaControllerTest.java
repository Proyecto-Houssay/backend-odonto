package com.proyectohoussay.odonto.clinicalhistory;

import com.proyectohoussay.odonto.clinicalhistory.dto.HistoriaClinicaRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HistoriaClinicaController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("HistoriaClinicaController")
class HistoriaClinicaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HistoriaClinicaService service;

    @Test
    @DisplayName("POST /api/historias-clinicas creates and returns 201 Created")
    void postCreatesHistoriaClinica() throws Exception {
        HistoriaClinica historia = new HistoriaClinica(LocalDate.now(), 5L, "Sin antecedentes");
        when(service.crearHistoriaClinica(any(HistoriaClinicaRequest.class))).thenReturn(historia);

        mockMvc.perform(post("/api/historias-clinicas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "pacienteId": 5,
                                  "observaciones": "Sin antecedentes"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pacienteId").value(5))
                .andExpect(jsonPath("$.observaciones").value("Sin antecedentes"));

        verify(service).crearHistoriaClinica(any(HistoriaClinicaRequest.class));
    }

    @Test
    @DisplayName("GET /api/historias-clinicas/paciente/{pacienteId} returns 200 OK")
    void getPorPacienteIdReturnsHistoriaClinica() throws Exception {
        HistoriaClinica historia = new HistoriaClinica(LocalDate.now(), 5L, "Sin antecedentes");
        when(service.obtenerPorPacienteId(5L)).thenReturn(historia);

        mockMvc.perform(get("/api/historias-clinicas/paciente/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pacienteId").value(5));
    }

    @Test
    @DisplayName("POST /api/historias-clinicas/{id}/diagnosticos adds diagnosis and returns 201 Created")
    void postAddsDiagnostico() throws Exception {
        Diagnostico diagnostico = new Diagnostico(LocalDate.now(), "Gingivitis");
        when(service.agregarDiagnostico(eq(1L), any(), eq("Gingivitis"), any())).thenReturn(diagnostico);

        mockMvc.perform(post("/api/historias-clinicas/1/diagnosticos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "historiaClinicaId": 1,
                                  "descripcion": "Gingivitis"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.descripcion").value("Gingivitis"));
    }
}

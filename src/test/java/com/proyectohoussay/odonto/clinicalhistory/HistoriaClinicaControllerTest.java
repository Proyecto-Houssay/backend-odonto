package com.proyectohoussay.odonto.clinicalhistory;

import com.proyectohoussay.odonto.clinicalhistory.dto.HistoriaClinicaResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    @DisplayName("GET /api/historias-clinicas/paciente/{pacienteId} returns 200 OK")
    void getPorPacienteIdReturnsHistoriaClinica() throws Exception {
        HistoriaClinicaResponse historia = new HistoriaClinicaResponse(null, LocalDate.now(), 5L,
                "Sin antecedentes", List.of());
        when(service.obtenerDetallePorPacienteId(5L)).thenReturn(historia);

        mockMvc.perform(get("/api/historias-clinicas/paciente/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pacienteId").value(5));
    }

    @Test
    @DisplayName("GET /api/historias-clinicas/{id}/detalle returns complete clinical history")
    void getDetalleReturnsHistoriaClinica() throws Exception {
        HistoriaClinicaResponse historia = new HistoriaClinicaResponse(10L, LocalDate.now(), 5L,
                "Sin antecedentes", List.of());
        when(service.obtenerDetallePorId(10L)).thenReturn(historia);

        mockMvc.perform(get("/api/historias-clinicas/10/detalle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pacienteId").value(5))
                .andExpect(jsonPath("$.observaciones").value("Sin antecedentes"));
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

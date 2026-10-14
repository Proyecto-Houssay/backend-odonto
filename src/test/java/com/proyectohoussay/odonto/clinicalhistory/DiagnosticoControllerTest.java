package com.proyectohoussay.odonto.clinicalhistory;

import com.proyectohoussay.odonto.clinicalhistory.dto.TratamientoDto;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DiagnosticoController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("DiagnosticoController")
class DiagnosticoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HistoriaClinicaService service;

    @Test
    @DisplayName("POST /api/diagnosticos creates diagnosis and returns 201 Created")
    void postCreatesDiagnostico() throws Exception {
        Diagnostico diagnostico = new Diagnostico(LocalDate.now(), "Caries oclusal");
        when(service.agregarDiagnostico(eq(2L), any(), eq("Caries oclusal"), any())).thenReturn(diagnostico);

        mockMvc.perform(post("/api/diagnosticos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "historiaClinicaId": 2,
                                  "descripcion": "Caries oclusal"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.descripcion").value("Caries oclusal"));
    }

    @Test
    @DisplayName("GET /api/diagnosticos/historia/{historiaClinicaId} lists diagnoses")
    void getListarPorHistoriaReturnsList() throws Exception {
        Diagnostico d = new Diagnostico(LocalDate.now(), "Gingivitis");
        when(service.listarDiagnosticosPorHistoria(2L)).thenReturn(List.of(d));

        mockMvc.perform(get("/api/diagnosticos/historia/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].descripcion").value("Gingivitis"));
    }

    @Test
    @DisplayName("POST /api/diagnosticos/{id}/tratamientos adds treatment and returns 201 Created")
    void postAddsTratamiento() throws Exception {
        Tratamiento t = new Tratamiento("Obturación composite", LocalDate.now(), EstadoTratamiento.PENDIENTE);
        when(service.agregarTratamiento(eq(3L), eq("Obturación composite"), any(), eq(EstadoTratamiento.PENDIENTE))).thenReturn(t);

        mockMvc.perform(post("/api/diagnosticos/3/tratamientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "descripcion": "Obturación composite",
                                  "estado": "PENDIENTE"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.descripcion").value("Obturación composite"))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"));
    }

    @Test
    @DisplayName("PATCH /api/diagnosticos/tratamientos/{id}/estado updates treatment status")
    void patchUpdatesTratamientoStatus() throws Exception {
        Tratamiento t = new Tratamiento("Obturación", LocalDate.now(), EstadoTratamiento.COMPLETADO);
        when(service.actualizarEstadoTratamiento(eq(4L), eq(EstadoTratamiento.COMPLETADO))).thenReturn(t);

        mockMvc.perform(patch("/api/diagnosticos/tratamientos/4/estado")
                        .param("nuevoEstado", "COMPLETADO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("COMPLETADO"));

        verify(service).actualizarEstadoTratamiento(4L, EstadoTratamiento.COMPLETADO);
    }
}

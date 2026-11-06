package com.proyectohoussay.odonto.clinicalhistory.ai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DiagnosticoImagenController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("DiagnosticoImagenController")
class DiagnosticoImagenControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DiagnosticoImagenService service;

    @Test
    @DisplayName("POST /api/ia/diagnostico-imagen/analizar returns 200 and analysis response")
    void postAnalizarReturnsAnalysis() throws Exception {
        AnalisisImagenResponse mockResponse = new AnalisisImagenResponse(
                1L,
                TipoImagenDental.RADIOGRAFIA_PANORAMICA,
                "Estudio panorámico completado.",
                List.of(new HallazgoImagenDto(
                        PatologiaDental.DIENTE_RETENIDO_IMPACTADO,
                        "Pieza 38",
                        SeveridadHallazgo.SEVERA,
                        0.95,
                        "Mandíbula",
                        "Impactado",
                        "Exodoncia"
                )),
                List.of("Exodoncia"),
                NivelUrgencia.ALTA,
                7.5,
                "Observaciones IA",
                DiagnosticoImagenService.MODELO_ACTUAL,
                LocalDateTime.now(),
                10L,
                5L,
                null
        );

        when(service.analizarImagen(any(AnalisisImagenRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/ia/diagnostico-imagen/analizar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tipoImagen": "RADIOGRAFIA_PANORAMICA",
                                  "pacienteId": 10,
                                  "guardarEnHistoria": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.tipoImagen").value("RADIOGRAFIA_PANORAMICA"))
                .andExpect(jsonPath("$.nivelUrgencia").value("ALTA"))
                .andExpect(jsonPath("$.hallazgos[0].piezaDental").value("Pieza 38"));

        verify(service).analizarImagen(any(AnalisisImagenRequest.class));
    }

    @Test
    @DisplayName("POST /api/ia/diagnostico-imagen/upload processes multipart image file")
    void postUploadProcessesMultipartFile() throws Exception {
        AnalisisImagenResponse mockResponse = new AnalisisImagenResponse(
                2L,
                TipoImagenDental.RADIOGRAFIA_PERIAPICAL,
                "Estudio periapical completado.",
                List.of(),
                List.of(),
                NivelUrgencia.BAJA,
                2.0,
                "OK",
                DiagnosticoImagenService.MODELO_ACTUAL,
                LocalDateTime.now(),
                15L,
                null,
                null
        );

        when(service.analizarArchivo(any(), eq(TipoImagenDental.RADIOGRAFIA_PERIAPICAL), any(), any(), any(), eq(false)))
                .thenReturn(mockResponse);

        MockMultipartFile file = new MockMultipartFile(
                "archivo",
                "periapical.png",
                "image/png",
                new byte[]{1, 2, 3, 4}
        );

        mockMvc.perform(multipart("/api/ia/diagnostico-imagen/upload")
                        .file(file)
                        .param("tipoImagen", "RADIOGRAFIA_PERIAPICAL")
                        .param("pacienteId", "15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.tipoImagen").value("RADIOGRAFIA_PERIAPICAL"));
    }

    @Test
    @DisplayName("GET /api/ia/diagnostico-imagen/catalogo returns 200 with available modalities")
    void getCatalogoReturnsAvailableModalities() throws Exception {
        CatalogoIaDto mockCatalogo = new CatalogoIaDto(
                List.of(CatalogoIaDto.TipoImagenInfo.de(TipoImagenDental.RADIOGRAFIA_PANORAMICA)),
                List.of(CatalogoIaDto.PatologiaInfo.de(PatologiaDental.CARIES_DENTAL)),
                List.of(CatalogoIaDto.UrgenciaInfo.de(NivelUrgencia.ALTA)),
                DiagnosticoImagenService.MODELO_ACTUAL,
                "OPERATIVO"
        );

        when(service.obtenerCatalogo()).thenReturn(mockCatalogo);

        mockMvc.perform(get("/api/ia/diagnostico-imagen/catalogo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoServicio").value("OPERATIVO"))
                .andExpect(jsonPath("$.tiposImagen[0].tipo").value("RADIOGRAFIA_PANORAMICA"));
    }

    @Test
    @DisplayName("GET /api/ia/diagnostico-imagen/paciente/{pacienteId} returns list of diagnoses")
    void getPorPacienteReturnsHistory() throws Exception {
        DiagnosticoImagen entidad = new DiagnosticoImagen(
                LocalDateTime.now(),
                10L,
                null,
                TipoImagenDental.RADIOGRAFIA_PANORAMICA,
                "foto.png",
                "Diagnóstico",
                NivelUrgencia.MEDIA,
                5.0,
                "[]",
                "Limpieza",
                null,
                null,
                "IA",
                null
        );

        when(service.obtenerHistorialPorPaciente(10L)).thenReturn(List.of(entidad));

        mockMvc.perform(get("/api/ia/diagnostico-imagen/paciente/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].pacienteId").value(10));
    }
}

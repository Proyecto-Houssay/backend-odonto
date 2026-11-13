package com.proyectohoussay.odonto.clinicalhistory.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyectohoussay.odonto.clinicalhistory.Diagnostico;
import com.proyectohoussay.odonto.clinicalhistory.DiagnosticoRepository;
import com.proyectohoussay.odonto.clinicalhistory.HistoriaClinica;
import com.proyectohoussay.odonto.clinicalhistory.HistoriaClinicaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DiagnosticoImagenService")
class DiagnosticoImagenServiceTest {

    @Mock
    private DiagnosticoImagenRepository diagnosticoImagenRepository;

    @Mock
    private HistoriaClinicaRepository historiaClinicaRepository;

    @Mock
    private DiagnosticoRepository diagnosticoRepository;

    private ObjectMapper objectMapper;
    private DiagnosticoImagenService service;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new DiagnosticoImagenService(
                diagnosticoImagenRepository,
                historiaClinicaRepository,
                diagnosticoRepository,
                objectMapper
        );
    }

    @Test
    @DisplayName("should return unavailable without a real analysis provider")
    void shouldAnalyzePanoramicRadiograph() {
        AnalisisImagenRequest request = new AnalisisImagenRequest(
                TipoImagenDental.RADIOGRAFIA_PANORAMICA,
                Base64.getEncoder().encodeToString("fake-panoramic-image".getBytes()),
                10L,
                null,
                "Paciente refiere molestia en zona retromolar",
                false,
                "panoramica.png"
        );

        var exception = assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> service.analizarImagen(request));
        assertEquals(503, exception.getStatusCode().value());
    }

    @Test
    @DisplayName("should not infer periapical findings when provider is unavailable")
    void shouldAnalyzePeriapicalRadiographWithUrgentStatus() {
        AnalisisImagenRequest request = new AnalisisImagenRequest(
                TipoImagenDental.RADIOGRAFIA_PERIAPICAL,
                null,
                20L,
                null,
                "Dolor agudo a la percusión en molar inferior",
                false,
                "periapical_36.jpg"
        );

        var exception = assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> service.analizarImagen(request));
        assertEquals(503, exception.getStatusCode().value());
    }

    @Test
    @DisplayName("should not infer bitewing findings when provider is unavailable")
    void shouldAnalyzeBitewingRadiograph() {
        AnalisisImagenRequest request = new AnalisisImagenRequest(
                TipoImagenDental.BITE_WING,
                null,
                15L,
                null,
                null,
                false,
                "aleta_mordida.png"
        );

        var exception = assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> service.analizarImagen(request));
        assertEquals(503, exception.getStatusCode().value());
    }

    @Test
    @DisplayName("should not persist a diagnosis when provider is unavailable")
    void shouldPersistDiagnosisIntoClinicalHistory() {
        AnalisisImagenRequest request = new AnalisisImagenRequest(
                TipoImagenDental.RADIOGRAFIA_PERIAPICAL,
                null,
                50L,
                null,
                "Chequeo de conducto",
                true,
                "estudio.png"
        );

        var exception = assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> service.analizarImagen(request));
        assertEquals(503, exception.getStatusCode().value());
        org.mockito.Mockito.verifyNoInteractions(diagnosticoImagenRepository, historiaClinicaRepository, diagnosticoRepository);
    }

    @Test
    @DisplayName("should remain unavailable without inspecting base64 content")
    void shouldRejectInvalidBase64() {
        AnalisisImagenRequest request = new AnalisisImagenRequest(
                TipoImagenDental.RADIOGRAFIA_PANORAMICA,
                "not-valid-base64-!!!???",
                1L,
                null,
                null,
                false,
                null
        );

        var exception = assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> service.analizarImagen(request));
        assertEquals(503, exception.getStatusCode().value());
    }

    @Test
    @DisplayName("should not read or save a multipart file before provider setup")
    void shouldProcessMultipartUpload() {
        MockMultipartFile file = new MockMultipartFile(
                "archivo",
                "radiografia.jpg",
                "image/jpeg",
                new byte[]{1, 2, 3, 4, 5, 6, 7, 8}
        );

        var exception = assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> service.analizarArchivo(
                file,
                TipoImagenDental.FOTO_INTRAORAL,
                12L,
                null,
                "Evaluación de cálculo dental",
                false
        ));
        assertEquals(503, exception.getStatusCode().value());
        org.mockito.Mockito.verifyNoInteractions(diagnosticoImagenRepository, historiaClinicaRepository, diagnosticoRepository);
    }

    @Test
    @DisplayName("should report Dr. Annie as not available")
    void shouldReturnCompleteCatalog() {
        CatalogoIaDto catalogo = service.obtenerCatalogo();

        assertNotNull(catalogo);
        assertTrue(catalogo.tiposImagen().isEmpty());
        assertTrue(catalogo.patologias().isEmpty());
        assertTrue(catalogo.nivelesUrgencia().isEmpty());
        assertEquals("EN_PREPARACION", catalogo.estadoServicio());
    }
}

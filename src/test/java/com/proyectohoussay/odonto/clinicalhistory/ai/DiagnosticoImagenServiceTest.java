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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    @DisplayName("should analyze panoramic radiograph and detect impacted molars and periodontitis")
    void shouldAnalyzePanoramicRadiograph() {
        when(diagnosticoImagenRepository.save(any(DiagnosticoImagen.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AnalisisImagenRequest request = new AnalisisImagenRequest(
                TipoImagenDental.RADIOGRAFIA_PANORAMICA,
                Base64.getEncoder().encodeToString("fake-panoramic-image".getBytes()),
                10L,
                null,
                "Paciente refiere molestia en zona retromolar",
                false,
                "panoramica.png"
        );

        AnalisisImagenResponse response = service.analizarImagen(request);

        assertNotNull(response);
        assertEquals(TipoImagenDental.RADIOGRAFIA_PANORAMICA, response.tipoImagen());
        assertFalse(response.hallazgos().isEmpty());
        assertTrue(response.hallazgos().stream().anyMatch(h -> h.patologia() == PatologiaDental.DIENTE_RETENIDO_IMPACTADO));
        assertTrue(response.hallazgos().stream().anyMatch(h -> h.patologia() == PatologiaDental.PERDIDA_OSEA_PERIODONTAL));
        assertEquals(NivelUrgencia.ALTA, response.nivelUrgencia());
        assertTrue(response.indiceSeveridad() > 5.0);
        assertNotNull(response.diagnosticoSugerido());
        assertFalse(response.tratamientosSugeridos().isEmpty());
    }

    @Test
    @DisplayName("should analyze periapical radiograph and set URGENTE urgency for periapical lesion")
    void shouldAnalyzePeriapicalRadiographWithUrgentStatus() {
        when(diagnosticoImagenRepository.save(any(DiagnosticoImagen.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AnalisisImagenRequest request = new AnalisisImagenRequest(
                TipoImagenDental.RADIOGRAFIA_PERIAPICAL,
                null,
                20L,
                null,
                "Dolor agudo a la percusión en molar inferior",
                false,
                "periapical_36.jpg"
        );

        AnalisisImagenResponse response = service.analizarImagen(request);

        assertNotNull(response);
        assertEquals(NivelUrgencia.URGENTE, response.nivelUrgencia());
        assertTrue(response.hallazgos().stream().anyMatch(h -> h.patologia() == PatologiaDental.LESION_PERIAPICAL));
        assertTrue(response.tratamientosSugeridos().stream().anyMatch(t -> t.contains("Endodoncia") || t.contains("conducto")));
    }

    @Test
    @DisplayName("should analyze bitewing radiograph and detect interproximal caries")
    void shouldAnalyzeBitewingRadiograph() {
        when(diagnosticoImagenRepository.save(any(DiagnosticoImagen.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AnalisisImagenRequest request = new AnalisisImagenRequest(
                TipoImagenDental.BITE_WING,
                null,
                15L,
                null,
                null,
                false,
                "aleta_mordida.png"
        );

        AnalisisImagenResponse response = service.analizarImagen(request);

        assertNotNull(response);
        assertEquals(TipoImagenDental.BITE_WING, response.tipoImagen());
        assertTrue(response.hallazgos().stream().anyMatch(h -> h.patologia() == PatologiaDental.CARIES_DENTAL));
        assertEquals(NivelUrgencia.MEDIA, response.nivelUrgencia());
    }

    @Test
    @DisplayName("should automatically persist diagnosis into clinical history when guardarEnHistoria is true")
    void shouldPersistDiagnosisIntoClinicalHistory() {
        HistoriaClinica historia = new HistoriaClinica(LocalDate.now(), 50L, "Historia previa");
        when(historiaClinicaRepository.findByPacienteId(50L)).thenReturn(Optional.of(historia));
        when(diagnosticoRepository.save(any(Diagnostico.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(diagnosticoImagenRepository.save(any(DiagnosticoImagen.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AnalisisImagenRequest request = new AnalisisImagenRequest(
                TipoImagenDental.RADIOGRAFIA_PERIAPICAL,
                null,
                50L,
                null,
                "Chequeo de conducto",
                true,
                "estudio.png"
        );

        AnalisisImagenResponse response = service.analizarImagen(request);

        assertNotNull(response);
        ArgumentCaptor<Diagnostico> captor = ArgumentCaptor.forClass(Diagnostico.class);
        verify(diagnosticoRepository).save(captor.capture());
        Diagnostico savedDiagnostico = captor.getValue();
        assertNotNull(savedDiagnostico);
        assertTrue(savedDiagnostico.getDescripcion().contains("Radiografía Periapical"));
        assertFalse(savedDiagnostico.getTratamientos().isEmpty());
    }

    @Test
    @DisplayName("should reject invalid base64 image data")
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

        assertThrows(IllegalArgumentException.class, () -> service.analizarImagen(request));
    }

    @Test
    @DisplayName("should process multipart uploaded file successfully")
    void shouldProcessMultipartUpload() {
        when(diagnosticoImagenRepository.save(any(DiagnosticoImagen.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MockMultipartFile file = new MockMultipartFile(
                "archivo",
                "radiografia.jpg",
                "image/jpeg",
                new byte[]{1, 2, 3, 4, 5, 6, 7, 8}
        );

        AnalisisImagenResponse response = service.analizarArchivo(
                file,
                TipoImagenDental.FOTO_INTRAORAL,
                12L,
                null,
                "Evaluación de cálculo dental",
                false
        );

        assertNotNull(response);
        assertEquals(TipoImagenDental.FOTO_INTRAORAL, response.tipoImagen());
        assertTrue(response.hallazgos().stream().anyMatch(h -> h.patologia() == PatologiaDental.CALCULO_DENTAL));
    }

    @Test
    @DisplayName("should return full catalog of AI capabilities")
    void shouldReturnCompleteCatalog() {
        CatalogoIaDto catalogo = service.obtenerCatalogo();

        assertNotNull(catalogo);
        assertEquals(5, catalogo.tiposImagen().size());
        assertEquals(7, catalogo.patologias().size());
        assertEquals(4, catalogo.nivelesUrgencia().size());
        assertEquals("OPERATIVO", catalogo.estadoServicio());
    }
}

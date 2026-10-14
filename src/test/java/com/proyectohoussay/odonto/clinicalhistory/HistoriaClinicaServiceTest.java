package com.proyectohoussay.odonto.clinicalhistory;

import com.proyectohoussay.odonto.clinicalhistory.dto.HistoriaClinicaRequest;
import com.proyectohoussay.odonto.clinicalhistory.dto.TratamientoDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("HistoriaClinicaService")
class HistoriaClinicaServiceTest {

    @Mock
    private HistoriaClinicaRepository historiaClinicaRepository;

    @Mock
    private DiagnosticoRepository diagnosticoRepository;

    @Mock
    private TratamientoRepository tratamientoRepository;

    private HistoriaClinicaService service;

    @BeforeEach
    void setUp() {
        service = new HistoriaClinicaService(
                historiaClinicaRepository,
                diagnosticoRepository,
                tratamientoRepository
        );
    }

    @Test
    @DisplayName("should create new clinical history for a patient")
    void shouldCreateHistoriaClinica() {
        when(historiaClinicaRepository.existsByPacienteId(1L)).thenReturn(false);
        when(historiaClinicaRepository.save(any(HistoriaClinica.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        HistoriaClinicaRequest request = new HistoriaClinicaRequest(1L, LocalDate.now(), "Sin antecedentes relevantes");
        HistoriaClinica creada = service.crearHistoriaClinica(request);

        assertNotNull(creada);
        assertEquals(1L, creada.getPacienteId());
        assertEquals("Sin antecedentes relevantes", creada.getObservaciones());
        verify(historiaClinicaRepository).save(any(HistoriaClinica.class));
    }

    @Test
    @DisplayName("should reject creating clinical history if patient already has one")
    void shouldRejectDuplicateHistoriaClinica() {
        when(historiaClinicaRepository.existsByPacienteId(1L)).thenReturn(true);

        HistoriaClinicaRequest request = new HistoriaClinicaRequest(1L, LocalDate.now(), "Observaciones");

        assertThrows(IllegalStateException.class, () -> service.crearHistoriaClinica(request));
    }

    @Test
    @DisplayName("should add diagnosis with treatments to clinical history")
    void shouldAddDiagnosisToHistoriaClinica() {
        HistoriaClinica historia = new HistoriaClinica(LocalDate.now(), 1L, "Observaciones");
        when(historiaClinicaRepository.findById(10L)).thenReturn(Optional.of(historia));
        when(diagnosticoRepository.save(any(Diagnostico.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TratamientoDto tDto = new TratamientoDto(null, "Obturación resina", LocalDate.now(), EstadoTratamiento.PENDIENTE);
        Diagnostico diagnostico = service.agregarDiagnostico(10L, LocalDate.now(), "Caries oclusal pieza 16", List.of(tDto));

        assertNotNull(diagnostico);
        assertEquals("Caries oclusal pieza 16", diagnostico.getDescripcion());
        assertEquals(1, diagnostico.getTratamientos().size());
        verify(diagnosticoRepository).save(any(Diagnostico.class));
    }

    @Test
    @DisplayName("should update treatment status")
    void shouldUpdateTreatmentStatus() {
        Tratamiento t = new Tratamiento("Endodoncia", LocalDate.now(), EstadoTratamiento.PENDIENTE);
        when(tratamientoRepository.findById(5L)).thenReturn(Optional.of(t));
        when(tratamientoRepository.save(any(Tratamiento.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Tratamiento actualizado = service.actualizarEstadoTratamiento(5L, EstadoTratamiento.COMPLETADO);

        assertNotNull(actualizado);
        assertEquals(EstadoTratamiento.COMPLETADO, actualizado.getEstado());
        verify(tratamientoRepository).save(t);
    }
}

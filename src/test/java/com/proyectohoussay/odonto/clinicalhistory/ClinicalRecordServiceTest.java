package com.proyectohoussay.odonto.clinicalhistory;

import com.proyectohoussay.odonto.patient.PacienteRepository;
import com.proyectohoussay.odonto.patient.Paciente;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class ClinicalRecordServiceTest {
    @Mock PacienteRepository pacienteRepository;
    @Mock HistoriaClinicaRepository historiaRepository;
    @Mock DiagnosticoRepository diagnosticoRepository;
    @Mock TratamientoRepository tratamientoRepository;
    @InjectMocks ClinicalRecordService service;

    @Test
    void refusesHistoryForUnknownPatient() {
        assertThatThrownBy(() -> service.createHistory(99L, LocalDate.now(), "Control"))
                .isInstanceOf(ClinicalRecordNotFoundException.class);
        verify(pacienteRepository).findLockedById(99L);
        verify(historiaRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void refusesSecondHistoryForSamePatient() {
        when(pacienteRepository.findLockedById(7L)).thenReturn(Optional.of(new Paciente()));
        when(historiaRepository.existsByPacienteId(7L)).thenReturn(true);
        assertThatThrownBy(() -> service.createHistory(7L, LocalDate.now(), "Control"))
                .isInstanceOf(ClinicalRecordConflictException.class);
    }

    @Test
    void convertsConcurrentDuplicateHistoryConstraintIntoConflict() {
        when(pacienteRepository.findLockedById(7L)).thenReturn(Optional.of(new Paciente()));
        when(historiaRepository.saveAndFlush(any(HistoriaClinica.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));
        assertThatThrownBy(() -> service.createHistory(7L, LocalDate.now(), "Control"))
                .isInstanceOf(ClinicalRecordConflictException.class);
    }

    @Test
    void createsDiagnosisUnderExistingHistory() {
        HistoriaClinica history = new HistoriaClinica(LocalDate.now(), 7L, null);
        when(historiaRepository.findById(3L)).thenReturn(Optional.of(history));
        when(diagnosticoRepository.save(any(Diagnostico.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Diagnostico diagnosis = service.createDiagnosis(3L, LocalDate.now(), "Caries");
        assertThat(diagnosis.getHistoriaClinica()).isSameAs(history);
        verify(diagnosticoRepository).save(diagnosis);
    }

    @Test
    void createsTreatmentUnderExistingDiagnosis() {
        Diagnostico diagnosis = new Diagnostico(LocalDate.now(), "Caries");
        when(diagnosticoRepository.findById(4L)).thenReturn(Optional.of(diagnosis));
        when(tratamientoRepository.save(any(Tratamiento.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Tratamiento treatment = service.createTreatment(4L, "Obturación", LocalDate.now(), EstadoTratamiento.PENDIENTE);
        assertThat(treatment.getDiagnostico()).isSameAs(diagnosis);
        verify(tratamientoRepository).save(treatment);
    }
}

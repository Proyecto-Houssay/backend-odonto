package com.proyectohoussay.odonto.patient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import org.springframework.dao.DataIntegrityViolationException;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.Optional;
import com.proyectohoussay.odonto.clinicalhistory.HistoriaClinicaRepository;
import com.proyectohoussay.odonto.repository.TurnoRepository;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PacienteServiceTest {

    @Mock
    private PacienteRepository pacienteRepository;

    @Mock
    private HistoriaClinicaRepository historiaClinicaRepository;

    @Mock
    private TurnoRepository turnoRepository;

    @InjectMocks
    private PacienteService pacienteService;

    @Test
    void registersPatientWhenDniIsNotAlreadyRegistered() {
        Paciente paciente = createPaciente("12345678");
        when(pacienteRepository.existsByDni("12345678")).thenReturn(false);
        when(pacienteRepository.saveAndFlush(paciente)).thenReturn(paciente);

        Paciente registrado = pacienteService.registrarPaciente(paciente);

        assertThat(registrado).isSameAs(paciente);
        verify(pacienteRepository).existsByDni("12345678");
        verify(pacienteRepository).saveAndFlush(paciente);
    }

    @Test
    void rejectsPatientWhenDniIsAlreadyRegistered() {
        Paciente paciente = createPaciente("12345678");
        when(pacienteRepository.existsByDni("12345678")).thenReturn(true);

        assertThatThrownBy(() -> pacienteService.registrarPaciente(paciente))
                .isInstanceOf(PacienteDuplicadoException.class)
                .hasMessage("Ya existe un paciente registrado con ese DNI");

        verify(pacienteRepository).existsByDni("12345678");
        verify(pacienteRepository, never()).saveAndFlush(paciente);
    }

    @Test
    void convertsConcurrentDuplicateDniDuringCreateToConflict() {
        Paciente patient = createPaciente("12345678");
        when(pacienteRepository.saveAndFlush(patient)).thenThrow(new DataIntegrityViolationException("duplicate DNI"));

        assertThatThrownBy(() -> pacienteService.registrarPaciente(patient))
                .isInstanceOf(PacienteDuplicadoException.class)
                .hasMessage("Ya existe un paciente registrado con ese DNI");
    }

    @Test
    void convertsConcurrentDuplicateDniDuringUpdateToConflict() {
        Paciente current = createPaciente("12345678");
        Paciente input = createPaciente("87654321");
        when(pacienteRepository.findById(5L)).thenReturn(Optional.of(current));
        when(pacienteRepository.saveAndFlush(current)).thenThrow(new DataIntegrityViolationException("duplicate DNI"));

        assertThatThrownBy(() -> pacienteService.actualizarPaciente(5L, input))
                .isInstanceOf(PacienteDuplicadoException.class)
                .hasMessage("Ya existe un paciente registrado con ese DNI");
    }

    @Test
    void updatesExistingPatientWithoutAllowingIdentityReplacement() {
        Paciente current = createPaciente("12345678");
        Paciente input = createPaciente("87654321");
        input.setNombre("Lucía");
        when(pacienteRepository.findById(5L)).thenReturn(Optional.of(current));
        when(pacienteRepository.saveAndFlush(current)).thenReturn(current);

        Paciente updated = pacienteService.actualizarPaciente(5L, input);

        assertThat(updated).isSameAs(current);
        assertThat(updated.getNombre()).isEqualTo("Lucía");
        assertThat(updated.getDni()).isEqualTo("87654321");
        verify(pacienteRepository).existsByDniAndIdNot("87654321", 5L);
    }

    @Test
    void deletionRefusesPatientWithClinicalHistory() {
        when(pacienteRepository.findLockedById(5L)).thenReturn(Optional.of(createPaciente("12345678")));
        when(historiaClinicaRepository.existsByPacienteId(5L)).thenReturn(true);

        assertThatThrownBy(() -> pacienteService.eliminarPaciente(5L))
                .isInstanceOf(IllegalStateException.class);
        verify(pacienteRepository).findLockedById(5L);
        verify(pacienteRepository, never()).deleteById(5L);
    }

    private Paciente createPaciente(String dni) {
        Paciente paciente = new Paciente();
        paciente.setNombre("Ana");
        paciente.setApellido("Pérez");
        paciente.setDni(dni);
        paciente.setTelefono("1123456789");
        paciente.setEmail("ana@example.com");
        paciente.setFechaNacimiento(LocalDate.of(1990, 1, 1));
        return paciente;
    }
}

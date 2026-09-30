package com.proyectohoussay.odonto.patient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

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

    @InjectMocks
    private PacienteService pacienteService;

    @Test
    void registersPatientWhenDniIsNotAlreadyRegistered() {
        Paciente paciente = createPaciente("12345678");
        when(pacienteRepository.existsByDni("12345678")).thenReturn(false);
        when(pacienteRepository.save(paciente)).thenReturn(paciente);

        Paciente registrado = pacienteService.registrarPaciente(paciente);

        assertThat(registrado).isSameAs(paciente);
        verify(pacienteRepository).existsByDni("12345678");
        verify(pacienteRepository).save(paciente);
    }

    @Test
    void rejectsPatientWhenDniIsAlreadyRegistered() {
        Paciente paciente = createPaciente("12345678");
        when(pacienteRepository.existsByDni("12345678")).thenReturn(true);

        assertThatThrownBy(() -> pacienteService.registrarPaciente(paciente))
                .isInstanceOf(PacienteDuplicadoException.class)
                .hasMessage("Ya existe un paciente registrado con ese DNI");

        verify(pacienteRepository).existsByDni("12345678");
        verify(pacienteRepository, never()).save(paciente);
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

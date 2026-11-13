package com.proyectohoussay.odonto.patient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

import java.util.Optional;

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
    private ObraSocialRepository obraSocialRepository;

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

    @Test
    void assignsCoverageAndMedicalAlerts() {
        Paciente paciente = createPaciente("12345678");
        org.springframework.test.util.ReflectionTestUtils.setField(paciente, "id", 10L);
        ObraSocial obraSocial = new ObraSocial("Salud Total", "ST", "ST-01", true);
        org.springframework.test.util.ReflectionTestUtils.setField(obraSocial, "id", 3L);
        when(pacienteRepository.findById(10L)).thenReturn(Optional.of(paciente));
        when(obraSocialRepository.findById(3L)).thenReturn(Optional.of(obraSocial));
        when(pacienteRepository.save(paciente)).thenReturn(paciente);

        Paciente updated = pacienteService.actualizarCobertura(10L, 3L, "AF-220", "Plan A", false);
        pacienteService.actualizarAlertasMedicas(10L, java.util.List.of("Alergia al látex", "Alergia al látex", "  "));

        assertThat(updated.isParticular()).isFalse();
        assertThat(updated.getObraSocial()).isEqualTo(obraSocial);
        assertThat(updated.getNumeroAfiliado()).isEqualTo("AF-220");
        assertThat(updated.getAlertasMedicas()).containsExactly("Alergia al látex");
        verify(pacienteRepository, times(2)).save(paciente);
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

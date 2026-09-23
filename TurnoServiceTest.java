package com.proyectohoussay.backendodonto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TurnoServiceTest {

    @Mock
    private TurnoRepository turnoRepository;

    @Mock
    private PacienteRepository pacienteRepository;

    @Mock
    private OdontologoRepository odontologoRepository;

    @InjectMocks
    private TurnoService turnoService;

    @Test
    void registraTurnoCorrectamente() {
        Paciente paciente = new Paciente(1L, "Juan", "Perez", "12345678");
        Odontologo odontologo = new Odontologo(1L, "Ana", "Gomez", "MP123");

        TurnoRequest request = new TurnoRequest(
                1L,
                1L,
                LocalDate.now().plusDays(1),
                LocalTime.of(10, 0),
                "Control"
        );

        when(pacienteRepository.findById(1L)).thenReturn(Optional.of(paciente));
        when(odontologoRepository.findById(1L)).thenReturn(Optional.of(odontologo));
        when(turnoRepository.existsByOdontologoIdAndFechaAndHorarioAndEstadoNot(
                eq(1L), eq(request.fecha()), eq(request.horario()), eq(EstadoTurno.CANCELADO)))
                .thenReturn(false);

        Turno turnoGuardado = new Turno();
        turnoGuardado.setId(100L);
        turnoGuardado.setPaciente(paciente);
        turnoGuardado.setOdontologo(odontologo);
        turnoGuardado.setFecha(request.fecha());
        turnoGuardado.setHorario(request.horario());
        turnoGuardado.setMotivo(request.motivo());
        turnoGuardado.setEstado(EstadoTurno.REGISTRADO);

        when(turnoRepository.save(any(Turno.class))).thenReturn(turnoGuardado);

        TurnoResponse response = turnoService.registrar(request);

        assertNotNull(response);
        assertEquals(100L, response.id());
        assertEquals("Turno registrado exitosamente", response.mensaje());
        assertEquals("REGISTRADO", response.estado());

        verify(turnoRepository).save(any(Turno.class));
    }

    @Test
    void fallaSiPacienteNoExiste() {
        TurnoRequest request = new TurnoRequest(
                99L,
                1L,
                LocalDate.now().plusDays(1),
                LocalTime.of(10, 0),
                "Control"
        );

        when(pacienteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> turnoService.registrar(request));
        verify(turnoRepository, never()).save(any());
    }

    @Test
    void fallaSiFechaEsAnterior() {
        Paciente paciente = new Paciente(1L, "Juan", "Perez", "12345678");
        Odontologo odontologo = new Odontologo(1L, "Ana", "Gomez", "MP123");

        TurnoRequest request = new TurnoRequest(
                1L,
                1L,
                LocalDate.now().minusDays(1),
                LocalTime.of(10, 0),
                "Control"
        );

        when(pacienteRepository.findById(1L)).thenReturn(Optional.of(paciente));
        when(odontologoRepository.findById(1L)).thenReturn(Optional.of(odontologo));

        assertThrows(FechaInvalidaException.class, () -> turnoService.registrar(request));
        verify(turnoRepository, never()).save(any());
    }

    @Test
    void fallaSiHorarioOcupado() {
        Paciente paciente = new Paciente(1L, "Juan", "Perez", "12345678");
        Odontologo odontologo = new Odontologo(1L, "Ana", "Gomez", "MP123");

        TurnoRequest request = new TurnoRequest(
                1L,
                1L,
                LocalDate.now().plusDays(1),
                LocalTime.of(10, 0),
                "Control"
        );

        when(pacienteRepository.findById(1L)).thenReturn(Optional.of(paciente));
        when(odontologoRepository.findById(1L)).thenReturn(Optional.of(odontologo));
        when(turnoRepository.existsByOdontologoIdAndFechaAndHorarioAndEstadoNot(
                eq(1L), eq(request.fecha()), eq(request.horario()), eq(EstadoTurno.CANCELADO)))
                .thenReturn(true);

        assertThrows(TurnoNoDisponibleException.class, () -> turnoService.registrar(request));
        verify(turnoRepository, never()).save(any());
    }
}
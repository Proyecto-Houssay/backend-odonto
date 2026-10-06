package com.proyectohoussay.odonto.service;

import com.proyectohoussay.odonto.dto.TurnoRequest;
import com.proyectohoussay.odonto.model.Odontologo;
import com.proyectohoussay.odonto.model.Turno;
import com.proyectohoussay.odonto.patient.Paciente;
import com.proyectohoussay.odonto.patient.PacienteRepository;
import com.proyectohoussay.odonto.repository.OdontologoRepository;
import com.proyectohoussay.odonto.repository.TurnoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TurnoServiceTest {

    private static final Long PACIENTE_ID = 10L;
    private static final Long ODONTOLOGO_ID = 20L;
    private static final LocalDate FECHA = LocalDate.now().plusDays(9);
    private static final LocalTime HORA = LocalTime.of(10, 30);

    @Mock
    private TurnoRepository turnoRepository;

    @Mock
    private PacienteRepository pacienteRepository;

    @Mock
    private OdontologoRepository odontologoRepository;

    @InjectMocks
    private TurnoService turnoService;

    @Test
    void createsTurnoWhenOdontologoIsAvailable() {
        Paciente paciente = mock(Paciente.class);
        Odontologo odontologo = new Odontologo();
        odontologo.setId(ODONTOLOGO_ID);
        TurnoRequest request = request();

        when(pacienteRepository.findById(PACIENTE_ID)).thenReturn(Optional.of(paciente));
        when(odontologoRepository.findById(ODONTOLOGO_ID)).thenReturn(Optional.of(odontologo));
        when(turnoRepository.existeTurnoActivoEnHorario(ODONTOLOGO_ID, FECHA, HORA, null)).thenReturn(false);
        when(turnoRepository.save(any(Turno.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Turno created = turnoService.crearTurno(request);

        assertThat(created.getFecha()).isEqualTo(FECHA);
        assertThat(created.getHora()).isEqualTo(HORA);
        assertThat(created.getPaciente()).isSameAs(paciente);
        assertThat(created.getOdontologo()).isSameAs(odontologo);
        verify(turnoRepository).save(created);
    }

    @Test
    void rejectsTurnoWhenOdontologoAlreadyHasTurnoAtSameDateAndTime() {
        Paciente paciente = mock(Paciente.class);
        Odontologo odontologo = new Odontologo();
        odontologo.setId(ODONTOLOGO_ID);

        when(pacienteRepository.findById(PACIENTE_ID)).thenReturn(Optional.of(paciente));
        when(odontologoRepository.findById(ODONTOLOGO_ID)).thenReturn(Optional.of(odontologo));
        when(turnoRepository.existeTurnoActivoEnHorario(ODONTOLOGO_ID, FECHA, HORA, null)).thenReturn(true);

        assertThatThrownBy(() -> turnoService.crearTurno(request()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("El odontólogo no tiene disponibilidad en la fecha y hora seleccionadas.");

        verify(turnoRepository).existeTurnoActivoEnHorario(ODONTOLOGO_ID, FECHA, HORA, null);
        verify(turnoRepository, never()).save(any(Turno.class));
    }

    @Test
    void rejectsTurnoWhenFechaIsInThePast() {
        LocalDate fechaPasada = LocalDate.now().minusDays(1);
        TurnoRequest pastRequest = new TurnoRequest(fechaPasada, HORA, "Urgencia", null, PACIENTE_ID, ODONTOLOGO_ID);

        assertThatThrownBy(() -> turnoService.crearTurno(pastRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("No se permiten turnos en fechas anteriores a la actual.");

        verify(turnoRepository, never()).save(any(Turno.class));
    }

    @Test
    void rejectsUpdateWhenFechaIsInThePast() {
        Turno current = existingTurno();
        when(turnoRepository.findById(5L)).thenReturn(java.util.Optional.of(current));
        TurnoRequest pastRequest = new TurnoRequest(LocalDate.now().minusDays(1), HORA,
                "Urgencia", null, PACIENTE_ID, ODONTOLOGO_ID);

        assertThatThrownBy(() -> turnoService.actualizarTurno(5L, pastRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("No se permiten turnos en fechas anteriores a la actual.");

        verify(turnoRepository, never()).save(any(Turno.class));
    }

    @Test
    void rejectsUpdateWhenAnotherActiveTurnoOccupiesNewSlot() {
        Turno current = existingTurno();
        when(turnoRepository.findById(5L)).thenReturn(java.util.Optional.of(current));
        when(pacienteRepository.findById(PACIENTE_ID)).thenReturn(java.util.Optional.of(current.getPaciente()));
        when(odontologoRepository.findById(ODONTOLOGO_ID)).thenReturn(java.util.Optional.of(current.getOdontologo()));
        when(turnoRepository.existeTurnoActivoEnHorario(ODONTOLOGO_ID, FECHA.plusDays(1), HORA, 5L))
                .thenReturn(true);

        TurnoRequest occupiedRequest = new TurnoRequest(FECHA.plusDays(1), HORA,
                "Control", null, PACIENTE_ID, ODONTOLOGO_ID);
        assertThatThrownBy(() -> turnoService.actualizarTurno(5L, occupiedRequest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no tiene disponibilidad");

        verify(turnoRepository, never()).save(any(Turno.class));
    }

    @Test
    void permitsUpdateKeepingItsOwnDentistDateAndTime() {
        Turno current = existingTurno();
        when(turnoRepository.findById(5L)).thenReturn(java.util.Optional.of(current));
        when(pacienteRepository.findById(PACIENTE_ID)).thenReturn(java.util.Optional.of(current.getPaciente()));
        when(odontologoRepository.findById(ODONTOLOGO_ID)).thenReturn(Optional.of(current.getOdontologo()));
        when(turnoRepository.existeTurnoActivoEnHorario(ODONTOLOGO_ID, FECHA, HORA, 5L)).thenReturn(false);
        when(turnoRepository.save(current)).thenReturn(current);

        Turno updated = turnoService.actualizarTurno(5L, request());

        assertThat(updated).isSameAs(current);
        verify(turnoRepository).existeTurnoActivoEnHorario(ODONTOLOGO_ID, FECHA, HORA, 5L);
        verify(turnoRepository).save(current);
    }

    @Test
    void validatesAvailabilityAgainstNewDentistWhenChangingDentist() {
        Long otherDentistId = 21L;
        Turno current = existingTurno();
        Odontologo otherDentist = new Odontologo();
        otherDentist.setId(otherDentistId);
        when(turnoRepository.findById(5L)).thenReturn(Optional.of(current));
        when(pacienteRepository.findById(PACIENTE_ID)).thenReturn(Optional.of(current.getPaciente()));
        when(odontologoRepository.findById(otherDentistId)).thenReturn(Optional.of(otherDentist));
        when(turnoRepository.existeTurnoActivoEnHorario(otherDentistId, FECHA, HORA, 5L)).thenReturn(false);
        when(turnoRepository.save(current)).thenReturn(current);

        TurnoRequest changedDentist = new TurnoRequest(FECHA, HORA, "Consulta", null,
                PACIENTE_ID, otherDentistId);
        Turno updated = turnoService.actualizarTurno(5L, changedDentist);

        assertThat(updated.getOdontologo()).isSameAs(otherDentist);
        verify(turnoRepository).existeTurnoActivoEnHorario(otherDentistId, FECHA, HORA, 5L);
    }

    @Test
    void cancellationMakesTheSlotAvailable() {
        when(turnoRepository.existeTurnoActivoEnHorario(ODONTOLOGO_ID, FECHA, HORA, null)).thenReturn(false);

        assertThat(turnoService.comprobarDisponibilidad(ODONTOLOGO_ID, FECHA, HORA)).isTrue();
    }

    private TurnoRequest request() {
        return new TurnoRequest(FECHA, HORA, "Consulta general", null, PACIENTE_ID, ODONTOLOGO_ID);
    }

    private Turno existingTurno() {
        Paciente paciente = mock(Paciente.class);
        Odontologo odontologo = new Odontologo();
        odontologo.setId(ODONTOLOGO_ID);
        Turno turno = new Turno(FECHA, HORA, "Consulta general", "PENDIENTE", paciente, odontologo);
        turno.setId(5L);
        return turno;
    }
}

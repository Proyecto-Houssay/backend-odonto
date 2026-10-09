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
    private static final LocalDate FECHA = LocalDate.now().plusDays(6);
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
        when(turnoRepository.existsByOdontologoIdAndFechaAndHora(ODONTOLOGO_ID, FECHA, HORA)).thenReturn(false);
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
        when(turnoRepository.existsByOdontologoIdAndFechaAndHora(ODONTOLOGO_ID, FECHA, HORA)).thenReturn(true);

        assertThatThrownBy(() -> turnoService.crearTurno(request()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("El odontólogo no tiene disponibilidad en la fecha y hora seleccionadas.");

        verify(turnoRepository).existsByOdontologoIdAndFechaAndHora(ODONTOLOGO_ID, FECHA, HORA);
        verify(turnoRepository, never()).save(any(Turno.class));
    }

    @Test
    void rejectsTurnoWhenDateIsInThePast() {
        TurnoRequest request = new TurnoRequest(
                LocalDate.now().minusDays(1), HORA, "Consulta general", null, PACIENTE_ID, ODONTOLOGO_ID);

        assertThatThrownBy(() -> turnoService.crearTurno(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La fecha del turno no puede ser anterior a la fecha actual.");

        verify(pacienteRepository, never()).findById(PACIENTE_ID);
        verify(odontologoRepository, never()).findById(ODONTOLOGO_ID);
        verify(turnoRepository, never()).save(any(Turno.class));
    }

    private TurnoRequest request() {
        return new TurnoRequest(FECHA, HORA, "Consulta general", null, PACIENTE_ID, ODONTOLOGO_ID);
    }
}

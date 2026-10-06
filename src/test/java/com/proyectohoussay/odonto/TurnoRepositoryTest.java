package com.proyectohoussay.odonto;

import com.proyectohoussay.odonto.model.Especialidad;
import com.proyectohoussay.odonto.model.Odontologo;
import com.proyectohoussay.odonto.model.Turno;
import com.proyectohoussay.odonto.patient.Paciente;
import com.proyectohoussay.odonto.patient.PacienteRepository;
import com.proyectohoussay.odonto.repository.EspecialidadRepository;
import com.proyectohoussay.odonto.repository.OdontologoRepository;
import com.proyectohoussay.odonto.repository.TurnoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class TurnoRepositoryTest {

    @Autowired
    private TurnoRepository turnoRepository;

    @Autowired
    private PacienteRepository pacienteRepository;

    @Autowired
    private OdontologoRepository odontologoRepository;

    @Autowired
    private EspecialidadRepository especialidadRepository;

    @Test
    void testGuardarYConsultarTurnosYDisponibilidad() {
        Paciente paciente = new Paciente();
        paciente.setNombre("Ana");
        paciente.setApellido("Lopez");
        paciente.setDni("30111222");
        paciente.setEmail("ana.lopez@example.com");
        paciente.setFechaNacimiento(LocalDate.of(1990, 1, 1));
        paciente = pacienteRepository.save(paciente);
        Especialidad esp = especialidadRepository.save(new Especialidad("Odontopediatría", "Atención dental a niños"));
        Odontologo odonto = odontologoRepository.save(new Odontologo("Laura", "Rios", "MN-9999", "laura@odonto.com", "1188776655", esp));

        LocalDate fecha = LocalDate.now().plusDays(9);
        LocalTime hora = LocalTime.of(10, 30);

        Turno turno = new Turno(fecha, hora, "Limpieza dental", "PENDIENTE", paciente, odonto);
        turnoRepository.save(turno);

        assertThat(turno.getId()).isNotNull();
        assertThat(turno.getPaciente().getId()).isEqualTo(paciente.getId());
        assertThat(turno.getOdontologo().getId()).isEqualTo(odonto.getId());

        List<Turno> turnosFecha = turnoRepository.findByFecha(fecha);
        assertThat(turnosFecha).hasSize(1);
        assertThat(turnosFecha.get(0).getMotivo()).isEqualTo("Limpieza dental");
        assertThat(turnoRepository.findByPacienteId(paciente.getId())).hasSize(1);
        assertThat(turnoRepository.findByOdontologoId(odonto.getId())).hasSize(1);

        boolean ocupado = turnoRepository.existeTurnoActivoEnHorario(odonto.getId(), fecha, hora, null);
        assertThat(ocupado).isTrue();

        boolean libreOtraHora = turnoRepository.existeTurnoActivoEnHorario(odonto.getId(), fecha, LocalTime.of(11, 0), null);
        assertThat(libreOtraHora).isFalse();

        turno.setEstado("CANCELADO");
        turnoRepository.save(turno);
        boolean libreTrasCancelar = turnoRepository.existeTurnoActivoEnHorario(odonto.getId(), fecha, hora, null);
        assertThat(libreTrasCancelar).isFalse();
    }
}

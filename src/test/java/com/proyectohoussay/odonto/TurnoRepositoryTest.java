package com.proyectohoussay.odonto;

import com.proyectohoussay.odonto.model.Especialidad;
import com.proyectohoussay.odonto.model.Odontologo;
import com.proyectohoussay.odonto.model.Turno;
import com.proyectohoussay.odonto.model.Usuario;
import com.proyectohoussay.odonto.repository.EspecialidadRepository;
import com.proyectohoussay.odonto.repository.OdontologoRepository;
import com.proyectohoussay.odonto.repository.TurnoRepository;
import com.proyectohoussay.odonto.repository.UsuarioRepository;
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
    private UsuarioRepository usuarioRepository;

    @Autowired
    private OdontologoRepository odontologoRepository;

    @Autowired
    private EspecialidadRepository especialidadRepository;

    @Test
    void testGuardarYConsultarTurnosYDisponibilidad() {
        Usuario paciente = usuarioRepository.save(new Usuario("Ana", "Lopez", "ana.lopez@example.com", "PACIENTE", true));
        Especialidad esp = especialidadRepository.save(new Especialidad("Odontopediatría", "Atención dental a niños"));
        Odontologo odonto = odontologoRepository.save(new Odontologo("Laura", "Rios", "MN-9999", "laura@odonto.com", "1188776655", esp));

        LocalDate fecha = LocalDate.of(2026, 10, 15);
        LocalTime hora = LocalTime.of(10, 30);

        Turno turno = new Turno(fecha, hora, "Limpieza dental", "PENDIENTE", paciente, odonto);
        turnoRepository.save(turno);

        List<Turno> turnosFecha = turnoRepository.findByFecha(fecha);
        assertThat(turnosFecha).hasSize(1);
        assertThat(turnosFecha.get(0).getMotivo()).isEqualTo("Limpieza dental");

        boolean ocupado = turnoRepository.existsByOdontologoIdAndFechaAndHora(odonto.getId(), fecha, hora);
        assertThat(ocupado).isTrue();

        boolean libreOtraHora = turnoRepository.existsByOdontologoIdAndFechaAndHora(odonto.getId(), fecha, LocalTime.of(11, 0));
        assertThat(libreOtraHora).isFalse();
    }
}

package com.proyectohoussay.odonto;

import com.proyectohoussay.odonto.model.Especialidad;
import com.proyectohoussay.odonto.model.Odontologo;
import com.proyectohoussay.odonto.model.DiaSemana;
import com.proyectohoussay.odonto.model.DisponibilidadHoraria;
import com.proyectohoussay.odonto.repository.EspecialidadRepository;
import com.proyectohoussay.odonto.repository.OdontologoRepository;
import com.proyectohoussay.odonto.service.OdontologoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static java.time.LocalTime.of;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class OdontologoServiceTest {

    @Mock
    private OdontologoRepository odontologoRepository;

    @Mock
    private EspecialidadRepository especialidadRepository;

    @InjectMocks
    private OdontologoService odontologoService;

    @Test
    void noRegistraOdontologoConEspecialidadInexistente() {
        Especialidad especialidad = new Especialidad("Ortodoncia", "Alineación dental");
        especialidad.setId(99L);
        Odontologo odontologo = new Odontologo("Ana", "Paz", "MN-101", null, null, especialidad);
        when(especialidadRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> odontologoService.crearOdontologo(odontologo))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("especialidad indicada no existe");

        verify(odontologoRepository, never()).save(odontologo);
    }

    @Test
    void noRegistraOdontologoConMatriculaDuplicada() {
        Especialidad especialidad = new Especialidad("Ortodoncia", "Alineación dental");
        especialidad.setId(1L);
        Odontologo odontologo = new Odontologo("Ana", "Paz", "MN-101", null, null, especialidad);
        when(especialidadRepository.existsById(1L)).thenReturn(true);
        when(odontologoRepository.existsByMatricula("MN-101")).thenReturn(true);

        assertThatThrownBy(() -> odontologoService.crearOdontologo(odontologo))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Ya existe un odontólogo");

        verify(odontologoRepository, never()).save(odontologo);
    }

    @Test
    void rejectsOverlappingWeeklyAvailabilityForTheSameDentist() {
        Especialidad especialidad = new Especialidad("Ortodoncia", "Alineación dental");
        especialidad.setId(1L);
        Odontologo odontologo = new Odontologo("Ana", "Paz", "MN-202", null, null, especialidad);
        when(odontologoRepository.findById(4L)).thenReturn(Optional.of(odontologo));
        List<DisponibilidadHoraria> solapadas = List.of(
                new DisponibilidadHoraria(DiaSemana.LUNES, of(9, 0), of(12, 0), 30),
                new DisponibilidadHoraria(DiaSemana.LUNES, of(11, 0), of(13, 0), 30));

        assertThatThrownBy(() -> odontologoService.actualizarDisponibilidad(4L, solapadas))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no pueden superponerse");

        verify(odontologoRepository, never()).save(odontologo);
    }

    @Test
    void rejectsAvailabilityWhenSharedConsultorioIsAlreadyOccupied() {
        Especialidad especialidad = new Especialidad("Ortodoncia", "Alineación dental");
        especialidad.setId(1L);
        Odontologo odontologo = new Odontologo("Ana", "Paz", "MN-203", null, null, especialidad);
        odontologo.setId(5L);
        odontologo.setConsultorio("Box 1");
        when(odontologoRepository.findById(5L)).thenReturn(Optional.of(odontologo));
        when(odontologoRepository.existeSolapamientoConsultorio("Box 1", DiaSemana.MARTES, of(9, 0), of(12, 0), 5L))
                .thenReturn(true);

        assertThatThrownBy(() -> odontologoService.actualizarDisponibilidad(5L,
                List.of(new DisponibilidadHoraria(DiaSemana.MARTES, of(9, 0), of(12, 0), 30))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("consultorio ya está ocupado");
    }
}

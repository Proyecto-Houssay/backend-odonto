package com.proyectohoussay.odonto;

import com.proyectohoussay.odonto.model.Especialidad;
import com.proyectohoussay.odonto.model.Odontologo;
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
}

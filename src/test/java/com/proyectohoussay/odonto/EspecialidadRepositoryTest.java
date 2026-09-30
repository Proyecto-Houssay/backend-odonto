package com.proyectohoussay.odonto;

import com.proyectohoussay.odonto.model.Especialidad;
import com.proyectohoussay.odonto.model.Odontologo;
import com.proyectohoussay.odonto.repository.EspecialidadRepository;
import com.proyectohoussay.odonto.repository.OdontologoRepository;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class EspecialidadRepositoryTest {

    @Autowired
    private EspecialidadRepository especialidadRepository;

    @Autowired
    private OdontologoRepository odontologoRepository;

    @Test
    void especialidadRequiereNombre() {
        assertThatThrownBy(() -> especialidadRepository.saveAndFlush(new Especialidad(" ", "Sin nombre")))
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void unaEspecialidadPuedeCompartirseEntreVariosOdontologos() {
        Especialidad especialidad = especialidadRepository.save(new Especialidad("Ortodoncia", "Alineación dental"));
        odontologoRepository.save(new Odontologo("Ana", "Paz", "MN-101", null, null, especialidad));
        odontologoRepository.save(new Odontologo("Luis", "Sol", "MN-102", null, null, especialidad));

        assertThat(odontologoRepository.findAll())
                .extracting(odontologo -> odontologo.getEspecialidad().getId())
                .containsOnly(especialidad.getId());
    }
}

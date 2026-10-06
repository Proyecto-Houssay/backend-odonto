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

    @Test
    void coleccionDeOdontologosNoSeExponeComoReferenciaMutable() {
        Especialidad especialidad = new Especialidad("Endodoncia", "Tratamiento de conductos");
        Odontologo odontologo = new Odontologo("Carlos", "Gomez", "MN-201", null, null, especialidad);

        assertThatThrownBy(() -> especialidad.getOdontologos().add(odontologo))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void helperMethodsMantienenConsistenciaDeLaRelacionBidireccional() {
        Especialidad especialidad = new Especialidad("Periodoncia", "Tratamiento de encías");
        Odontologo odontologo = new Odontologo("Marta", "Rios", "MN-301", null, null, null);

        especialidad.addOdontologo(odontologo);
        assertThat(especialidad.getOdontologos()).contains(odontologo);
        assertThat(odontologo.getEspecialidad()).isEqualTo(especialidad);

        especialidad.removeOdontologo(odontologo);
        assertThat(especialidad.getOdontologos()).doesNotContain(odontologo);
        assertThat(odontologo.getEspecialidad()).isNull();
    }

    @Test
    void odontologoRequiereNombreYApellido() {
        Especialidad especialidad = especialidadRepository.save(new Especialidad("Cirugía", "Cirugía maxilofacial"));

        assertThatThrownBy(() -> odontologoRepository.saveAndFlush(new Odontologo(" ", "Perez", "MN-401", null, null, especialidad)))
                .isInstanceOf(ConstraintViolationException.class);

        assertThatThrownBy(() -> odontologoRepository.saveAndFlush(new Odontologo("Pedro", " ", "MN-402", null, null, especialidad)))
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void especialidadValidaLimitesDeLongitud() {
        String nombreLargo = "E".repeat(101);
        assertThatThrownBy(() -> especialidadRepository.saveAndFlush(new Especialidad(nombreLargo, "Valida longitud")))
                .isInstanceOf(ConstraintViolationException.class);

        String descripcionLarga = "D".repeat(256);
        assertThatThrownBy(() -> especialidadRepository.saveAndFlush(new Especialidad("Ortopedia", descripcionLarga)))
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void nombreDeEspecialidadEsUnico() {
        especialidadRepository.saveAndFlush(new Especialidad("Endodoncia", "Conductos"));

        assertThatThrownBy(() -> especialidadRepository.saveAndFlush(new Especialidad("Endodoncia", "Otra descripción")))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }
}

package com.proyectohoussay.odonto;

import com.proyectohoussay.odonto.model.Especialidad;
import com.proyectohoussay.odonto.model.Odontologo;
import com.proyectohoussay.odonto.repository.EspecialidadRepository;
import com.proyectohoussay.odonto.repository.OdontologoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class OdontologoRepositoryTest {

    @Autowired
    private OdontologoRepository odontologoRepository;

    @Autowired
    private EspecialidadRepository especialidadRepository;

    @Test
    void testGuardarYBuscarOdontologoPorMatricula() {
        Especialidad esp = especialidadRepository.save(new Especialidad("Ortodoncia", "Especialidad en alineación dental"));

        Odontologo odontologo = new Odontologo("Carlos", "Perez", "MN-12345", "carlos.perez@odonto.com", "1122334455", esp);
        odontologoRepository.save(odontologo);

        Optional<Odontologo> porMatricula = odontologoRepository.findByMatricula("MN-12345");
        assertThat(porMatricula).isPresent();
        assertThat(porMatricula.get().getNombre()).isEqualTo("Carlos");
        assertThat(porMatricula.get().getEspecialidad().getNombre()).isEqualTo("Ortodoncia");

        boolean existe = odontologoRepository.existsByMatricula("MN-12345");
        assertThat(existe).isTrue();
    }

    @Test
    void testMatriculaDuplicadaLanzaExcepcion() {
        Especialidad esp = especialidadRepository.save(new Especialidad("Endodoncia", "Conductos"));

        Odontologo o1 = new Odontologo("Carlos", "Perez", "MN-99999", "carlos@test.com", "123456", esp);
        odontologoRepository.saveAndFlush(o1);

        Odontologo o2 = new Odontologo("Maria", "Lopez", "MN-99999", "maria@test.com", "654321", esp);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> odontologoRepository.saveAndFlush(o2))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    void testLongitudMaximaNombreYMatricula() {
        Especialidad esp = especialidadRepository.save(new Especialidad("Periodoncia", "Encias"));

        String nombreLargo = "A".repeat(101);
        Odontologo oNombreLargo = new Odontologo(nombreLargo, "Perez", "MN-88888", null, null, esp);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> odontologoRepository.saveAndFlush(oNombreLargo))
                .isInstanceOf(jakarta.validation.ConstraintViolationException.class);

        String matriculaLarga = "M".repeat(51);
        Odontologo oMatriculaLarga = new Odontologo("Carlos", "Perez", matriculaLarga, null, null, esp);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> odontologoRepository.saveAndFlush(oMatriculaLarga))
                .isInstanceOf(jakarta.validation.ConstraintViolationException.class);
    }

    @Test
    void testMoverOdontologoDeEspecialidadActualizaAmbasRelaciones() {
        Especialidad esp1 = new Especialidad("Cirugía", "Cirugía general");
        Especialidad esp2 = new Especialidad("Ortodoncia", "Alineación");

        Odontologo o = new Odontologo("Juan", "Gomez", "MN-77777", null, null, esp1);
        assertThat(esp1.getOdontologos()).contains(o);

        // Mover a esp2 usando setEspecialidad
        o.setEspecialidad(esp2);
        assertThat(o.getEspecialidad()).isEqualTo(esp2);
        assertThat(esp1.getOdontologos()).doesNotContain(o);
        assertThat(esp2.getOdontologos()).contains(o);

        // Quitar especialidad
        o.setEspecialidad(null);
        assertThat(o.getEspecialidad()).isNull();
        assertThat(esp2.getOdontologos()).doesNotContain(o);
    }
}

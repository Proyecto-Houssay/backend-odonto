package com.proyectohoussay.odonto;

import com.proyectohoussay.odonto.model.Especialidad;
import com.proyectohoussay.odonto.model.Odontologo;
import com.proyectohoussay.odonto.model.DiaSemana;
import com.proyectohoussay.odonto.model.DisponibilidadHoraria;
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

        Odontologo oApellidoLargo = new Odontologo("Carlos", "A".repeat(101), "MN-88889", null, null, esp);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> odontologoRepository.saveAndFlush(oApellidoLargo))
                .isInstanceOf(jakarta.validation.ConstraintViolationException.class);

        Odontologo oEmailLargo = new Odontologo("Carlos", "Perez", "MN-88890", "a".repeat(101), null, esp);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> odontologoRepository.saveAndFlush(oEmailLargo))
                .isInstanceOf(jakarta.validation.ConstraintViolationException.class);

        Odontologo oTelefonoLargo = new Odontologo("Carlos", "Perez", "MN-88891", null, "1".repeat(21), esp);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> odontologoRepository.saveAndFlush(oTelefonoLargo))
                .isInstanceOf(jakarta.validation.ConstraintViolationException.class);
    }

    @Test
    void testMoverOdontologoDeEspecialidadActualizaAmbasRelaciones() {
        Especialidad esp1 = especialidadRepository.save(new Especialidad("Cirugía", "Cirugía general"));
        Especialidad esp2 = especialidadRepository.save(new Especialidad("Ortodoncia", "Alineación"));

        Odontologo o = new Odontologo("Juan", "Gomez", "MN-77777", null, null, esp1);
        odontologoRepository.saveAndFlush(o);
        Long odontologoId = o.getId();
        assertThat(esp1.getOdontologos()).contains(o);

        o.setEspecialidad(esp2);
        odontologoRepository.saveAndFlush(o);

        assertThat(o.getEspecialidad()).isEqualTo(esp2);
        assertThat(esp1.getOdontologos()).doesNotContain(o);
        assertThat(esp2.getOdontologos()).contains(o);
        assertThat(odontologoRepository.findById(odontologoId).orElseThrow().getEspecialidad().getId())
                .isEqualTo(esp2.getId());
    }

    @Test
    void persistsWeeklyAvailability() {
        Especialidad esp = especialidadRepository.save(new Especialidad("Odontopediatría", "Atención infantil"));
        Odontologo odontologo = new Odontologo("Laura", "Díaz", "MN-65432", null, null, esp);
        odontologo.setConsultorio("Box 2");
        odontologo.setDisponibilidades(java.util.List.of(
                new DisponibilidadHoraria(DiaSemana.MIERCOLES, java.time.LocalTime.of(9, 0), java.time.LocalTime.of(13, 0), 30)));
        odontologoRepository.saveAndFlush(odontologo);

        Odontologo persisted = odontologoRepository.findById(odontologo.getId()).orElseThrow();
        assertThat(persisted.getConsultorio()).isEqualTo("Box 2");
        assertThat(persisted.getDisponibilidades()).hasSize(1);
        assertThat(persisted.getDisponibilidades().get(0).getHoraInicio()).isEqualTo(java.time.LocalTime.of(9, 0));
    }

    @Test
    void detectsOverlappingAvailabilityForTheSameConsultorio() {
        Especialidad esp = especialidadRepository.save(new Especialidad("Endodoncia", "Conductos"));
        Odontologo odontologo = new Odontologo("María", "León", "MN-66551", null, null, esp);
        odontologo.setConsultorio("Box A");
        odontologo.setDisponibilidades(java.util.List.of(
                new DisponibilidadHoraria(DiaSemana.JUEVES, java.time.LocalTime.of(9, 0), java.time.LocalTime.of(12, 0), 30)));
        odontologoRepository.saveAndFlush(odontologo);

        assertThat(odontologoRepository.existeSolapamientoConsultorio("Box A", DiaSemana.JUEVES,
                java.time.LocalTime.of(11, 0), java.time.LocalTime.of(13, 0), null)).isTrue();
        assertThat(odontologoRepository.existeSolapamientoConsultorio("Box B", DiaSemana.JUEVES,
                java.time.LocalTime.of(11, 0), java.time.LocalTime.of(13, 0), null)).isFalse();
    }
}

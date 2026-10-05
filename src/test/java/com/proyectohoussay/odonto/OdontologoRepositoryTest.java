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
}

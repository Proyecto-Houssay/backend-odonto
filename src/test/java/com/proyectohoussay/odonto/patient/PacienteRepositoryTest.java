package com.proyectohoussay.odonto.patient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
class PacienteRepositoryTest {

    @Autowired
    private PacienteRepository pacienteRepository;

    @Test
    void existsByDniReturnsTrueOnlyWhenPatientIsPersisted() {
        Paciente paciente = createPaciente("12345678");
        Paciente guardado = pacienteRepository.saveAndFlush(paciente);

        assertThat(pacienteRepository.existsByDni("12345678")).isTrue();
        assertThat(pacienteRepository.existsByDni("87654321")).isFalse();
        assertThat(pacienteRepository.findById(guardado.getId()))
                .get()
                .extracting(Paciente::getTelefono)
                .isEqualTo("1123456789");
    }

    @Test
    void rejectsDuplicateDniAtDatabaseLevel() {
        pacienteRepository.saveAndFlush(createPaciente("12345678"));

        assertThatThrownBy(() -> pacienteRepository.saveAndFlush(createPaciente("12345678")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Paciente createPaciente(String dni) {
        Paciente paciente = new Paciente();
        paciente.setNombre("Ana");
        paciente.setApellido("Pérez");
        paciente.setDni(dni);
        paciente.setTelefono("1123456789");
        paciente.setEmail("ana@example.com");
        paciente.setFechaNacimiento(LocalDate.of(1990, 1, 1));
        return paciente;
    }
}

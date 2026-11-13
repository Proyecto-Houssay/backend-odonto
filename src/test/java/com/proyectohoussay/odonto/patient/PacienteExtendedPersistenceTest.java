package com.proyectohoussay.odonto.patient;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class PacienteExtendedPersistenceTest {
    @Autowired private PacienteRepository pacienteRepository;
    @Autowired private ObraSocialRepository obraSocialRepository;

    @Test
    void persistsCoverageEmergencyContactAndMedicalAlerts() {
        ObraSocial obraSocial = obraSocialRepository.save(new ObraSocial("Cobertura Integral", "CI", "CI-01", true));
        Paciente paciente = new Paciente();
        paciente.setNombre("Julia");
        paciente.setApellido("Pérez");
        paciente.setDni("22114455");
        paciente.setEmail("julia@example.com");
        paciente.setFechaNacimiento(LocalDate.of(1988, 2, 3));
        paciente.setObraSocial(obraSocial);
        paciente.setNumeroAfiliado("AF-998");
        paciente.setPlanCobertura("Oro");
        paciente.setParticular(false);
        paciente.setContactoEmergencia(new ContactoEmergencia("Mario Pérez", "Padre", "11223344"));
        paciente.setAlertasMedicas(List.of("Alergia al látex", "Diabetes"));
        pacienteRepository.saveAndFlush(paciente);

        Paciente loaded = pacienteRepository.findByDni("22114455").orElseThrow();
        assertThat(loaded.getObraSocial().getNombre()).isEqualTo("Cobertura Integral");
        assertThat(loaded.getNumeroAfiliado()).isEqualTo("AF-998");
        assertThat(loaded.getContactoEmergencia().getTelefono()).isEqualTo("11223344");
        assertThat(loaded.getAlertasMedicas()).containsExactly("Alergia al látex", "Diabetes");
        assertThat(loaded.getCreadoEn()).isNotNull();
        assertThat(loaded.getActualizadoEn()).isNotNull();
    }
}

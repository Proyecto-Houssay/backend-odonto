package com.proyectohoussay.odonto.historia;

import com.proyectohoussay.odonto.diagnostico.Diagnostico;
import com.proyectohoussay.odonto.diagnostico.DiagnosticoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class HistoriaClinicaRepositoryTest {

    @Autowired
    private HistoriaClinicaRepository historiaClinicaRepository;

    @Autowired
    private DiagnosticoRepository diagnosticoRepository;

    @Test
    void deberiaGuardarHistoriaClinicaConVariosDiagnosticos() {

        HistoriaClinica historiaClinica = new HistoriaClinica(
                LocalDate.of(2026, 9, 21),
                "Paciente presenta evolucion clinica favorable.",
                1L
        );

        Diagnostico diagnostico1 = new Diagnostico(
                "Caries dental",
                historiaClinica
        );

        Diagnostico diagnostico2 = new Diagnostico(
                "Gingivitis",
                historiaClinica
        );

        historiaClinica.agregarDiagnostico(diagnostico1);
        historiaClinica.agregarDiagnostico(diagnostico2);

        HistoriaClinica guardada =
                historiaClinicaRepository.save(historiaClinica);

        historiaClinicaRepository.flush();

        HistoriaClinica recuperada =
                historiaClinicaRepository.findById(guardada.getId())
                        .orElseThrow();

        assertThat(recuperada.getId()).isNotNull();

        assertThat(recuperada.getPacienteId())
                .isEqualTo(1L);

        assertThat(recuperada.getFechaApertura())
                .isEqualTo(LocalDate.of(2026, 9, 21));

        assertThat(recuperada.getObservaciones())
                .isEqualTo("Paciente presenta evolucion clinica favorable.");

        assertThat(recuperada.getDiagnosticos())
                .hasSize(2);

        assertThat(diagnosticoRepository.count())
                .isEqualTo(2);
    }
}

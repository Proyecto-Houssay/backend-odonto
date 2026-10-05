package com.proyectohoussay.odonto.historia;

import com.proyectohoussay.odonto.diagnostico.Diagnostico;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class HistoriaClinicaRepositoryTest {

    @Autowired
    private HistoriaClinicaRepository historiaClinicaRepository;

    @Test
    void debeGuardarHistoriaClinicaConDiagnosticos() {

        LocalDate fecha = LocalDate.now();

        HistoriaClinica historia = new HistoriaClinica(
                fecha,
                "Paciente en control odontológico general.",
                1L
        );

        Diagnostico diagnostico1 = new Diagnostico(
                fecha,
                "Caries dental",
                historia
        );

        Diagnostico diagnostico2 = new Diagnostico(
                fecha,
                "Gingivitis",
                historia
        );

        historia.agregarDiagnostico(diagnostico1);
        historia.agregarDiagnostico(diagnostico2);

        HistoriaClinica guardada = historiaClinicaRepository.save(historia);

        assertNotNull(guardada);
        assertNotNull(guardada.getId());

        assertEquals(fecha, guardada.getFechaApertura());
        assertEquals(1L, guardada.getPacienteId());
        assertEquals(
                "Paciente en control odontológico general.",
                guardada.getObservaciones()
        );

        assertNotNull(guardada.getDiagnosticos());
        assertEquals(2, guardada.getDiagnosticos().size());

        assertEquals(
                "Caries dental",
                guardada.getDiagnosticos().get(0).getDescripcion()
        );

        assertEquals(
                "Gingivitis",
                guardada.getDiagnosticos().get(1).getDescripcion()
        );

        assertEquals(
                guardada,
                guardada.getDiagnosticos().get(0).getHistoriaClinica()
        );

        assertEquals(
                guardada,
                guardada.getDiagnosticos().get(1).getHistoriaClinica()
        );
    }
}
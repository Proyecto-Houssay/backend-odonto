package com.proyectohoussay.odonto.clinicalhistory.ai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DataJpaTest
@DisplayName("DiagnosticoImagenRepository")
class DiagnosticoImagenRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private DiagnosticoImagenRepository repository;

    @Test
    @DisplayName("should persist and retrieve image diagnosis entity by patient ID")
    void shouldPersistAndRetrieveByPacienteId() {
        DiagnosticoImagen entidad = new DiagnosticoImagen(
                LocalDateTime.now(),
                100L,
                50L,
                TipoImagenDental.RADIOGRAFIA_PANORAMICA,
                "panoramica_test.png",
                "Diagnóstico sugerido de prueba",
                NivelUrgencia.ALTA,
                7.2,
                "[]",
                "Exodoncia | Obturación",
                "Observaciones odontólogo",
                "Observaciones IA",
                "Novadent AI",
                null
        );

        entityManager.persistAndFlush(entidad);

        assertNotNull(entidad.getId());

        List<DiagnosticoImagen> encontrados = repository.findAllByPacienteIdOrderByFechaAnalisisDesc(100L);
        assertFalse(encontrados.isEmpty());
        assertEquals(1, encontrados.size());
        assertEquals(TipoImagenDental.RADIOGRAFIA_PANORAMICA, encontrados.get(0).getTipoImagen());
        assertEquals(NivelUrgencia.ALTA, encontrados.get(0).getNivelUrgencia());
    }
}

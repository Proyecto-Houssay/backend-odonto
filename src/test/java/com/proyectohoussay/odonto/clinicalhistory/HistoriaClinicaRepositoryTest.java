package com.proyectohoussay.odonto.clinicalhistory;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@DisplayName("HistoriaClinicaRepository")
class HistoriaClinicaRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private HistoriaClinicaRepository repository;

    @Test
    @DisplayName("should persist a clinical history with required fields")
    void shouldPersistHistoriaClinica() {
        HistoriaClinica historia = new HistoriaClinica(
                LocalDate.of(2026, 9, 1),
                1L,
                "First visit observations"
        );

        HistoriaClinica saved = repository.save(historia);
        entityManager.flush();

        assertNotNull(saved.getId());
        assertEquals(LocalDate.of(2026, 9, 1), saved.getFechaApertura());
        assertEquals(1L, saved.getPacienteId());
        assertEquals("First visit observations", saved.getObservaciones());
    }

    @Test
    @DisplayName("should find clinical history by patient ID")
    void shouldFindByPacienteId() {
        HistoriaClinica historia = new HistoriaClinica(
                LocalDate.now(), 42L, null
        );
        entityManager.persistAndFlush(historia);

        Optional<HistoriaClinica> found = repository.findByPacienteId(42L);

        assertTrue(found.isPresent());
        assertEquals(42L, found.get().getPacienteId());
    }

    @Test
    @DisplayName("should return empty when patient has no clinical history")
    void shouldReturnEmptyWhenNoHistoryForPatient() {
        Optional<HistoriaClinica> found = repository.findByPacienteId(999L);

        assertFalse(found.isPresent());
    }

    @Test
    @DisplayName("should confirm existence by patient ID")
    void shouldCheckExistsByPacienteId() {
        HistoriaClinica historia = new HistoriaClinica(
                LocalDate.now(), 10L, "Check-up"
        );
        entityManager.persistAndFlush(historia);

        assertTrue(repository.existsByPacienteId(10L));
        assertFalse(repository.existsByPacienteId(11L));
    }

    @Test
    @DisplayName("should persist clinical history without observations")
    void shouldPersistWithoutObservaciones() {
        HistoriaClinica historia = new HistoriaClinica(
                LocalDate.now(), 5L, null
        );

        HistoriaClinica saved = repository.save(historia);
        entityManager.flush();

        assertNotNull(saved.getId());
        assertEquals(5L, saved.getPacienteId());
    }

    @Test
    @DisplayName("should cover relationship with Diagnostico and cascade persistence")
    void shouldCoverRelationshipWithDiagnostico() {
        HistoriaClinica historia = new HistoriaClinica(
                LocalDate.now(), 77L, "Historial de ortodoncia"
        );
        Diagnostico diagnostico = new Diagnostico(
                LocalDate.now(), "Maloclusion clase II"
        );
        historia.agregarDiagnostico(diagnostico);

        HistoriaClinica saved = repository.save(historia);
        entityManager.flush();
        entityManager.clear();

        Optional<HistoriaClinica> found = repository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals(1, found.get().getDiagnosticos().size());
        assertEquals("Maloclusion clase II", found.get().getDiagnosticos().get(0).getDescripcion());
        assertEquals(saved.getId(), found.get().getDiagnosticos().get(0).getHistoriaClinica().getId());
    }
}

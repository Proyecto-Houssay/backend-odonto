package com.proyectohoussay.odonto.clinicalhistory;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DataJpaTest
@DisplayName("DiagnosticoRepository")
class DiagnosticoRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private DiagnosticoRepository diagnosticoRepository;

    @Test
    @DisplayName("should persist a diagnosis linked to a clinical history")
    void shouldPersistDiagnostico() {
        HistoriaClinica historia = new HistoriaClinica(
                LocalDate.now(), 1L, null
        );
        entityManager.persistAndFlush(historia);

        Diagnostico diagnostico = new Diagnostico(
                LocalDate.now(), "Caries in tooth 36"
        );
        historia.agregarDiagnostico(diagnostico);
        entityManager.persistAndFlush(diagnostico);

        assertNotNull(diagnostico.getId());
        assertEquals(historia.getId(), diagnostico.getHistoriaClinica().getId());
    }

    @Test
    @DisplayName("should find all diagnoses by clinical history ID")
    void shouldFindAllByHistoriaClinicaId() {
        HistoriaClinica historia = new HistoriaClinica(
                LocalDate.now(), 1L, null
        );
        entityManager.persistAndFlush(historia);

        Diagnostico d1 = new Diagnostico(LocalDate.now(), "Caries");
        Diagnostico d2 = new Diagnostico(LocalDate.now(), "Gingivitis");
        historia.agregarDiagnostico(d1);
        historia.agregarDiagnostico(d2);
        entityManager.persistAndFlush(d1);
        entityManager.persistAndFlush(d2);

        List<Diagnostico> found = diagnosticoRepository
                .findAllByHistoriaClinicaId(historia.getId());

        assertEquals(2, found.size());
    }

    @Test
    @DisplayName("should return empty list when no diagnoses exist for history")
    void shouldReturnEmptyWhenNoDiagnoses() {
        HistoriaClinica historia = new HistoriaClinica(
                LocalDate.now(), 1L, null
        );
        entityManager.persistAndFlush(historia);

        List<Diagnostico> found = diagnosticoRepository
                .findAllByHistoriaClinicaId(historia.getId());

        assertNotNull(found);
        assertTrue(found.isEmpty());
    }

    private void assertTrue(boolean condition) {
        org.junit.jupiter.api.Assertions.assertTrue(condition);
    }

    @Test
    @DisplayName("should cascade diagnostics through clinical history")
    void shouldCascadeThroughHistoriaClinica() {
        HistoriaClinica historia = new HistoriaClinica(
                LocalDate.now(), 1L, "First visit"
        );
        Diagnostico diagnostico = new Diagnostico(
                LocalDate.now(), "Periodontitis"
        );
        historia.agregarDiagnostico(diagnostico);

        entityManager.persistAndFlush(historia);

        assertNotNull(diagnostico.getId());
        assertFalse(historia.getDiagnosticos().isEmpty());
        assertEquals("Periodontitis", historia.getDiagnosticos().get(0).getDescripcion());
    }

    @Test
    @DisplayName("should verify relationship between Diagnostico and Tratamientos")
    void shouldVerifyRelationshipWithTratamientos() {
        HistoriaClinica historia = new HistoriaClinica(
                LocalDate.now(), 15L, "Control preventivo"
        );
        entityManager.persistAndFlush(historia);

        Diagnostico diagnostico = new Diagnostico(
                LocalDate.now(), "Gingivitis marginal"
        );
        historia.agregarDiagnostico(diagnostico);
        entityManager.persistAndFlush(diagnostico);

        Tratamiento t1 = new Tratamiento(
                "Tartrectomia", LocalDate.now(), EstadoTratamiento.EN_CURSO
        );
        Tratamiento t2 = new Tratamiento(
                "Instruccion de higiene", LocalDate.now(), EstadoTratamiento.PENDIENTE
        );
        diagnostico.agregarTratamiento(t1);
        diagnostico.agregarTratamiento(t2);

        entityManager.persistAndFlush(t1);
        entityManager.persistAndFlush(t2);
        entityManager.clear();

        Diagnostico found = entityManager.find(Diagnostico.class, diagnostico.getId());
        assertNotNull(found);
        assertEquals(2, found.getTratamientos().size());
        assertTrue(found.getTratamientos().stream().anyMatch(t -> t.getDescripcion().equals("Tartrectomia")));
        assertTrue(found.getTratamientos().stream().anyMatch(t -> t.getEstado() == EstadoTratamiento.EN_CURSO));
        assertTrue(found.getTratamientos().stream().allMatch(t -> t.getDiagnostico().getId().equals(diagnostico.getId())));
    }
}

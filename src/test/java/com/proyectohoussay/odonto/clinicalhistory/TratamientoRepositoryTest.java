package com.proyectohoussay.odonto.clinicalhistory;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@DisplayName("TratamientoRepository")
class TratamientoRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TratamientoRepository tratamientoRepository;

    private Diagnostico createPersistedDiagnostico() {
        HistoriaClinica historia = new HistoriaClinica(
                LocalDate.now(), 1L, null
        );
        entityManager.persistAndFlush(historia);

        Diagnostico diagnostico = new Diagnostico(
                LocalDate.now(), "Caries"
        );
        historia.agregarDiagnostico(diagnostico);
        entityManager.persistAndFlush(diagnostico);

        return diagnostico;
    }

    @Test
    @DisplayName("should persist a treatment linked to a diagnosis")
    void shouldPersistTratamiento() {
        Diagnostico diagnostico = createPersistedDiagnostico();

        Tratamiento tratamiento = new Tratamiento(
                "Composite filling", LocalDate.now(), EstadoTratamiento.PENDIENTE
        );
        diagnostico.agregarTratamiento(tratamiento);
        entityManager.persistAndFlush(tratamiento);

        assertNotNull(tratamiento.getId());
        assertEquals(diagnostico.getId(), tratamiento.getDiagnostico().getId());
        assertEquals(EstadoTratamiento.PENDIENTE, tratamiento.getEstado());
    }

    @Test
    @DisplayName("should find all treatments by diagnosis ID")
    void shouldFindAllByDiagnosticoId() {
        Diagnostico diagnostico = createPersistedDiagnostico();

        Tratamiento t1 = new Tratamiento(
                "Filling", LocalDate.now(), EstadoTratamiento.PENDIENTE
        );
        Tratamiento t2 = new Tratamiento(
                "Root canal", LocalDate.now(), EstadoTratamiento.EN_CURSO
        );
        diagnostico.agregarTratamiento(t1);
        diagnostico.agregarTratamiento(t2);
        entityManager.persistAndFlush(t1);
        entityManager.persistAndFlush(t2);

        List<Tratamiento> found = tratamientoRepository
                .findAllByDiagnosticoId(diagnostico.getId());

        assertEquals(2, found.size());
    }

    @Test
    @DisplayName("should find treatments by estado")
    void shouldFindByEstado() {
        Diagnostico diagnostico = createPersistedDiagnostico();

        Tratamiento pending = new Tratamiento(
                "Filling", LocalDate.now(), EstadoTratamiento.PENDIENTE
        );
        Tratamiento completed = new Tratamiento(
                "Cleaning", LocalDate.now(), EstadoTratamiento.COMPLETADO
        );
        diagnostico.agregarTratamiento(pending);
        diagnostico.agregarTratamiento(completed);
        entityManager.persistAndFlush(pending);
        entityManager.persistAndFlush(completed);

        List<Tratamiento> pendingList = tratamientoRepository
                .findAllByEstado(EstadoTratamiento.PENDIENTE);
        List<Tratamiento> completedList = tratamientoRepository
                .findAllByEstado(EstadoTratamiento.COMPLETADO);

        assertEquals(1, pendingList.size());
        assertEquals(1, completedList.size());
        assertEquals("Filling", pendingList.get(0).getDescripcion());
    }

    @Test
    @DisplayName("should update treatment estado")
    void shouldUpdateEstado() {
        Diagnostico diagnostico = createPersistedDiagnostico();

        Tratamiento tratamiento = new Tratamiento(
                "Extraction", LocalDate.now(), EstadoTratamiento.PENDIENTE
        );
        diagnostico.agregarTratamiento(tratamiento);
        entityManager.persistAndFlush(tratamiento);

        tratamiento.setEstado(EstadoTratamiento.COMPLETADO);
        entityManager.persistAndFlush(tratamiento);

        Tratamiento updated = entityManager.find(Tratamiento.class, tratamiento.getId());
        assertEquals(EstadoTratamiento.COMPLETADO, updated.getEstado());
    }

    @Test
    @DisplayName("should cascade treatments through diagnosis")
    void shouldCascadeThroughDiagnostico() {
        HistoriaClinica historia = new HistoriaClinica(
                LocalDate.now(), 1L, null
        );
        Diagnostico diagnostico = new Diagnostico(
                LocalDate.now(), "Caries"
        );
        Tratamiento tratamiento = new Tratamiento(
                "Filling", LocalDate.now(), EstadoTratamiento.PENDIENTE
        );

        historia.agregarDiagnostico(diagnostico);
        diagnostico.agregarTratamiento(tratamiento);

        entityManager.persistAndFlush(historia);

        assertNotNull(tratamiento.getId());
        assertEquals(1, diagnostico.getTratamientos().size());
    }

    @Test
    @DisplayName("should return empty list for non-existent diagnosis")
    void shouldReturnEmptyForNonExistentDiagnosis() {
        List<Tratamiento> found = tratamientoRepository
                .findAllByDiagnosticoId(999L);

        assertNotNull(found);
        assertTrue(found.isEmpty());
    }
}

package com.houssay.odonto.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.persistence.PersistenceException;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

@DataJpaTest
class TratamientoMappingTest {

    @Autowired
    private TestEntityManager em;

    // TODO: ajustar a la entidad Diagnostico real del proyecto
    private Diagnostico crearDiagnostico() {
        Diagnostico d = new Diagnostico();
        return em.persistAndFlush(d);
    }

    @Test
    void persisteTratamientoConDiagnostico() {
        Diagnostico d = crearDiagnostico();
        Tratamiento t = new Tratamiento("Endodoncia pieza 36",
                LocalDate.of(2026, 9, 23), EstadoTratamiento.PLANIFICADO, d);

        Tratamiento guardado = em.persistFlushFind(t);

        assertThat(guardado.getId()).isNotNull();
        assertThat(guardado.getDiagnostico().getId()).isEqualTo(d.getId());
        assertThat(guardado.getEstado()).isEqualTo(EstadoTratamiento.PLANIFICADO);
    }

    @Test
    void guardaEstadoComoTexto() {
        Tratamiento t = new Tratamiento("Limpieza", LocalDate.now(),
                EstadoTratamiento.EN_CURSO, crearDiagnostico());
        em.persistAndFlush(t);

        Object valor = em.getEntityManager()
                .createNativeQuery("select estado from tratamientos where id = :id")
                .setParameter("id", t.getId())
                .getSingleResult();

        assertThat(valor).isEqualTo("EN_CURSO");
    }

    @Test
    void rechazaTratamientoSinDiagnostico() {
        Tratamiento t = new Tratamiento("Sin diagnóstico", LocalDate.now(),
                EstadoTratamiento.PLANIFICADO, null);

        assertThatThrownBy(() -> em.persistAndFlush(t))
                .isInstanceOf(PersistenceException.class);
    }

    @Test
    void rechazaDescripcionNula() {
        Tratamiento t = new Tratamiento(null, LocalDate.now(),
                EstadoTratamiento.PLANIFICADO, crearDiagnostico());

        assertThatThrownBy(() -> em.persistAndFlush(t))
                .isInstanceOf(PersistenceException.class);
    }

    @Test
    void rechazaDescripcionMayorA500() {
        Tratamiento t = new Tratamiento("x".repeat(501), LocalDate.now(),
                EstadoTratamiento.PLANIFICADO, crearDiagnostico());

        assertThatThrownBy(() -> em.persistAndFlush(t))
                .isInstanceOf(PersistenceException.class);
    }
}

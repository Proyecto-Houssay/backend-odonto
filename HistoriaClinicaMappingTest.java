package ar.houssay.odonto.historiaclinica;

import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
class HistoriaClinicaMappingTest {

    @Autowired
    private TestEntityManager em;

    private HistoriaClinica historiaValida() {
        return new HistoriaClinica(1L, LocalDate.of(2026, 9, 23), "Primera consulta");
    }

    @Test
    void persisteHistoriaConMultiplesDiagnosticos() {
        HistoriaClinica historia = historiaValida();
        historia.agregarDiagnostico(new Diagnostico("Caries en pieza 16", LocalDate.of(2026, 9, 23)));
        historia.agregarDiagnostico(new Diagnostico("Gingivitis leve", LocalDate.of(2026, 9, 23)));

        em.persistAndFlush(historia);
        em.clear();

        HistoriaClinica leida = em.find(HistoriaClinica.class, historia.getId());
        assertThat(leida.getPacienteId()).isEqualTo(1L);
        assertThat(leida.getFechaApertura()).isEqualTo(LocalDate.of(2026, 9, 23));
        assertThat(leida.getDiagnosticos()).hasSize(2);
        assertThat(leida.getDiagnosticos())
                .allSatisfy(d -> assertThat(d.getHistoriaClinica().getId()).isEqualTo(leida.getId()));
    }

    @Test
    void eliminaDiagnosticoHuerfano() {
        HistoriaClinica historia = historiaValida();
        Diagnostico d1 = new Diagnostico("Caries", LocalDate.of(2026, 9, 23));
        Diagnostico d2 = new Diagnostico("Gingivitis", LocalDate.of(2026, 9, 23));
        historia.agregarDiagnostico(d1);
        historia.agregarDiagnostico(d2);
        em.persistAndFlush(historia);

        historia.quitarDiagnostico(d1);
        em.flush();
        em.clear();

        assertThat(em.find(HistoriaClinica.class, historia.getId()).getDiagnosticos()).hasSize(1);
        assertThat(em.find(Diagnostico.class, d1.getId())).isNull();
    }

    @Test
    void rechazaPacienteNulo() {
        HistoriaClinica historia = new HistoriaClinica(null, LocalDate.now(), null);
        assertThrows(PersistenceException.class, () -> em.persistAndFlush(historia));
    }

    @Test
    void rechazaFechaAperturaNula() {
        HistoriaClinica historia = new HistoriaClinica(1L, null, null);
        assertThrows(PersistenceException.class, () -> em.persistAndFlush(historia));
    }

    @Test
    void rechazaObservacionesMuyLargas() {
        String larga = "x".repeat(HistoriaClinica.OBSERVACIONES_MAX + 1);
        HistoriaClinica historia = new HistoriaClinica(1L, LocalDate.now(), larga);
        assertThrows(PersistenceException.class, () -> em.persistAndFlush(historia));
    }

    @Test
    void rechazaDiagnosticoSinDescripcion() {
        HistoriaClinica historia = historiaValida();
        historia.agregarDiagnostico(new Diagnostico(null, LocalDate.now()));
        assertThrows(PersistenceException.class, () -> em.persistAndFlush(historia));
    }

    @Test
    void rechazaDescripcionDeDiagnosticoMuyLarga() {
        HistoriaClinica historia = historiaValida();
        historia.agregarDiagnostico(new Diagnostico(
                "x".repeat(Diagnostico.DESCRIPCION_MAX + 1), LocalDate.now()));
        assertThrows(PersistenceException.class, () -> em.persistAndFlush(historia));
    }
}

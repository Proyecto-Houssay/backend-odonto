package com.proyectohoussay.odonto.repository;

import com.proyectohoussay.odonto.model.CargoPersonal;
import com.proyectohoussay.odonto.model.Personal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@DisplayName("PersonalRepository")
class PersonalRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PersonalRepository personalRepository;

    @Test
    @DisplayName("should persist and find personal by DNI and cargo")
    void shouldPersistAndFindByDniAndCargo() {
        Personal p = new Personal(
                "Carlos",
                "Vega",
                "34567890",
                "carlos@novadent.com",
                "1144556677",
                CargoPersonal.RECEPCIONISTA,
                LocalDate.now(),
                "08:00 a 16:00",
                null
        );

        entityManager.persistAndFlush(p);
        assertNotNull(p.getId());

        Optional<Personal> byDni = personalRepository.findByDni("34567890");
        assertTrue(byDni.isPresent());
        assertEquals("Carlos", byDni.get().getNombre());
        assertEquals(CargoPersonal.RECEPCIONISTA, byDni.get().getCargo());

        assertTrue(personalRepository.existsByDni("34567890"));
        assertFalse(personalRepository.existsByDni("99999999"));

        List<Personal> recepcionistas = personalRepository.findAllByCargo(CargoPersonal.RECEPCIONISTA);
        assertEquals(1, recepcionistas.size());
    }

    @Test
    @DisplayName("should find personal by usuarioId")
    void shouldFindByUsuarioId() {
        Personal p = new Personal(
                "Marta",
                "Suárez",
                "28999888",
                "marta@novadent.com",
                null,
                CargoPersonal.ASISTENTE_DENTAL,
                LocalDate.now(),
                null,
                15L
        );

        entityManager.persistAndFlush(p);

        Optional<Personal> byUser = personalRepository.findByUsuarioId(15L);
        assertTrue(byUser.isPresent());
        assertEquals("Marta", byUser.get().getNombre());
    }
}

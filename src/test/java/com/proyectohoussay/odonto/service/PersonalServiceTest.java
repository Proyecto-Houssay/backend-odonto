package com.proyectohoussay.odonto.service;

import com.proyectohoussay.odonto.dto.PersonalRequest;
import com.proyectohoussay.odonto.model.CargoPersonal;
import com.proyectohoussay.odonto.model.Personal;
import com.proyectohoussay.odonto.repository.PersonalRepository;
import com.proyectohoussay.odonto.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PersonalService")
class PersonalServiceTest {

    @Mock
    private PersonalRepository personalRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    private PersonalService personalService;

    @BeforeEach
    void setUp() {
        personalService = new PersonalService(personalRepository, usuarioRepository);
    }

    @Test
    @DisplayName("should create personal successfully when DNI is unique and user exists")
    void shouldCreatePersonalSuccessfully() {
        when(personalRepository.existsByDni("34567890")).thenReturn(false);
        when(usuarioRepository.existsById(10L)).thenReturn(true);
        when(personalRepository.save(any(Personal.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PersonalRequest request = new PersonalRequest(
                "Carlos",
                "Vega",
                "34567890",
                "carlos@novadent.com",
                "1144556677",
                CargoPersonal.RECEPCIONISTA,
                LocalDate.now(),
                "Lunes a Viernes 08:00 a 16:00",
                10L,
                true
        );

        Personal creado = personalService.crearPersonal(request);

        assertNotNull(creado);
        assertEquals("Carlos", creado.getNombre());
        assertEquals("34567890", creado.getDni());
        assertEquals(CargoPersonal.RECEPCIONISTA, creado.getCargo());
        assertEquals(10L, creado.getUsuarioId());
        assertTrue(creado.isActivo());
        verify(personalRepository).save(any(Personal.class));
    }

    @Test
    @DisplayName("should throw IllegalStateException when DNI already exists")
    void shouldThrowWhenDniAlreadyExists() {
        when(personalRepository.existsByDni("34567890")).thenReturn(true);

        PersonalRequest request = new PersonalRequest(
                "Carlos",
                "Vega",
                "34567890",
                "carlos@novadent.com",
                null,
                CargoPersonal.RECEPCIONISTA,
                null,
                null,
                null,
                null
        );

        assertThrows(IllegalStateException.class, () -> personalService.crearPersonal(request));
    }

    @Test
    @DisplayName("should throw IllegalArgumentException when usuarioId does not exist")
    void shouldThrowWhenUsuarioIdDoesNotExist() {
        when(personalRepository.existsByDni("34567890")).thenReturn(false);
        when(usuarioRepository.existsById(999L)).thenReturn(false);

        PersonalRequest request = new PersonalRequest(
                "Carlos",
                "Vega",
                "34567890",
                null,
                null,
                CargoPersonal.ASISTENTE_DENTAL,
                null,
                null,
                999L,
                null
        );

        assertThrows(IllegalArgumentException.class, () -> personalService.crearPersonal(request));
    }

    @Test
    @DisplayName("should update personal details successfully")
    void shouldUpdatePersonalDetails() {
        Personal existente = new Personal(
                "Carlos", "Vega", "34567890", "carlos@novadent.com", "1144556677",
                CargoPersonal.RECEPCIONISTA, LocalDate.now(), "Mañana", 10L
        );
        existente.setId(1L);

        when(personalRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(personalRepository.save(any(Personal.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PersonalRequest updateReq = new PersonalRequest(
                "Carlos Alberto",
                "Vega",
                "34567890",
                "carlos.nuevo@novadent.com",
                "1199887766",
                CargoPersonal.SECRETARIO,
                null,
                "Tarde",
                10L,
                true
        );

        Personal actualizado = personalService.actualizarPersonal(1L, updateReq);

        assertNotNull(actualizado);
        assertEquals("Carlos Alberto", actualizado.getNombre());
        assertEquals(CargoPersonal.SECRETARIO, actualizado.getCargo());
        assertEquals("Tarde", actualizado.getHorarioLaboral());
    }

    @Test
    @DisplayName("should perform soft delete when darDeBaja is called")
    void shouldSoftDeletePersonal() {
        Personal p = new Personal("Ana", "López", "40112233", "ana@novadent.com", null, CargoPersonal.ASISTENTE_DENTAL, LocalDate.now(), null, null);
        p.setId(2L);
        when(personalRepository.findById(2L)).thenReturn(Optional.of(p));
        when(personalRepository.save(any(Personal.class))).thenAnswer(invocation -> invocation.getArgument(0));

        personalService.darDeBaja(2L);

        assertFalse(p.isActivo());
        verify(personalRepository).save(p);
    }
}

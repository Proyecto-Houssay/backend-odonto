package com.proyectohoussay.odonto.service;

import com.proyectohoussay.odonto.dto.MovimientoFinancieroRequest;
import com.proyectohoussay.odonto.model.MovimientoFinanciero;
import com.proyectohoussay.odonto.model.TipoMovimientoFinanciero;
import com.proyectohoussay.odonto.repository.MovimientoFinancieroRepository;
import com.proyectohoussay.odonto.repository.TurnoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MovimientoFinancieroServiceTest {

    private MovimientoFinancieroRepository movimientoRepository;
    private TurnoRepository turnoRepository;
    private MovimientoFinancieroService service;

    @BeforeEach
    void setUp() {
        movimientoRepository = mock(MovimientoFinancieroRepository.class);
        turnoRepository = mock(TurnoRepository.class);
        service = new MovimientoFinancieroService(movimientoRepository, turnoRepository);
    }

    @Test
    void ingresoDebeVincularseAUnTurno() {
        assertThatThrownBy(() -> service.registrar(request(TipoMovimientoFinanciero.INGRESO, null)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(error -> ((ResponseStatusException) error).getStatusCode().value())
                .isEqualTo(400);

        verify(movimientoRepository, never()).save(any(MovimientoFinanciero.class));
    }

    @Test
    void egresoNoPuedeVincularseAUnTurno() {
        assertThatThrownBy(() -> service.registrar(request(TipoMovimientoFinanciero.EGRESO, 9L)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(error -> ((ResponseStatusException) error).getStatusCode().value())
                .isEqualTo(400);

        verify(movimientoRepository, never()).save(any(MovimientoFinanciero.class));
    }

    @Test
    void registraEgresoNormalizandoElConceptoYConImportePositivo() {
        when(movimientoRepository.save(any(MovimientoFinanciero.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.registrar(request(TipoMovimientoFinanciero.EGRESO, null));

        assertThat(response.tipo()).isEqualTo(TipoMovimientoFinanciero.EGRESO);
        assertThat(response.importe()).isEqualByComparingTo("2500.00");
        assertThat(response.concepto()).isEqualTo("Compra de materiales");
        verify(turnoRepository, never()).findById(any());
    }

    @Test
    void ingresoConTurnoInexistenteRetorna404YNoPersiste() {
        when(turnoRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registrar(request(TipoMovimientoFinanciero.INGRESO, 9L)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(error -> ((ResponseStatusException) error).getStatusCode().value())
                .isEqualTo(404);

        verify(movimientoRepository, never()).save(any(MovimientoFinanciero.class));
    }

    @Test
    void rechazoDePeriodoInvertidoNoConsultaElRepositorio() {
        LocalDate today = LocalDate.now();

        assertThatThrownBy(() -> service.cobros(today, today.minusDays(1)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(error -> ((ResponseStatusException) error).getStatusCode().value())
                .isEqualTo(400);

        verify(movimientoRepository, never()).findByFechaBetweenAndTipo(
                any(LocalDate.class), any(LocalDate.class), any(TipoMovimientoFinanciero.class));
    }

    private MovimientoFinancieroRequest request(TipoMovimientoFinanciero tipo, Long turnoId) {
        return new MovimientoFinancieroRequest(tipo, LocalDate.now(), new BigDecimal("2500.00"),
                "  Compra de materiales  ", "EFECTIVO", turnoId);
    }
}

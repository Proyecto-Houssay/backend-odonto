package com.proyectohoussay.odonto.service;

import com.proyectohoussay.odonto.model.MovimientoFinanciero;
import com.proyectohoussay.odonto.model.TipoMovimientoFinanciero;
import com.proyectohoussay.odonto.repository.InsumoRepository;
import com.proyectohoussay.odonto.repository.MovimientoFinancieroRepository;
import com.proyectohoussay.odonto.repository.TurnoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReportFinancialServiceTest {

    private MovimientoFinancieroRepository movimientoRepository;
    private ReportService service;

    @BeforeEach
    void setUp() {
        movimientoRepository = mock(MovimientoFinancieroRepository.class);
        service = new ReportService(mock(InsumoRepository.class), mock(TurnoRepository.class),
                movimientoRepository, mock(MovimientoFinancieroService.class));
    }

    @Test
    void gananciasCalculaIngresoEgresoYResultadoNetoDelPeriodo() {
        LocalDate desde = LocalDate.of(2026, 4, 1);
        LocalDate hasta = LocalDate.of(2026, 4, 30);
        when(movimientoRepository.findByFechaBetween(desde, hasta)).thenReturn(List.of(
                movement(TipoMovimientoFinanciero.INGRESO, "12500.00"),
                movement(TipoMovimientoFinanciero.INGRESO, "500.00"),
                movement(TipoMovimientoFinanciero.EGRESO, "2500.00")));

        Map<String, Object> report = service.obtenerReporteGanancias(desde, hasta);

        assertThat(report).containsEntry("ingresosRegistrados", new BigDecimal("13000.00"))
                .containsEntry("egresosRegistrados", new BigDecimal("2500.00"))
                .containsEntry("gananciaNetaRegistrada", new BigDecimal("10500.00"))
                .containsEntry("cantidadMovimientos", 3)
                .containsEntry("sinRegistros", false);
    }

    @Test
    void reportaResultadoCeroCuandoNoHayMovimientos() {
        LocalDate today = LocalDate.now();
        when(movimientoRepository.findByFechaBetween(today, today)).thenReturn(List.of());

        Map<String, Object> report = service.obtenerReporteGanancias(today, today);

        assertThat(report).containsEntry("ingresosRegistrados", BigDecimal.ZERO)
                .containsEntry("egresosRegistrados", BigDecimal.ZERO)
                .containsEntry("gananciaNetaRegistrada", BigDecimal.ZERO)
                .containsEntry("cantidadMovimientos", 0)
                .containsEntry("sinRegistros", true);
    }

    private MovimientoFinanciero movement(TipoMovimientoFinanciero tipo, String amount) {
        MovimientoFinanciero movement = new MovimientoFinanciero();
        movement.setTipo(tipo);
        movement.setImporte(new BigDecimal(amount));
        return movement;
    }
}

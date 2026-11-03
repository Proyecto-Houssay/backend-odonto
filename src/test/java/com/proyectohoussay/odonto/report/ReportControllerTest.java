package com.proyectohoussay.odonto.report;

import com.proyectohoussay.odonto.dto.InventarioItemDto;
import com.proyectohoussay.odonto.service.ReportService;
import com.proyectohoussay.odonto.auth.RoleAuthorizationInterceptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doReturn;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReportController.class)
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReportService reportService;

    @MockitoBean
    private RoleAuthorizationInterceptor roleAuthorizationInterceptor;

    @BeforeEach
    void allowAnnotatedRoutesInControllerSlice() throws Exception {
        doReturn(true).when(roleAuthorizationInterceptor).preHandle(any(), any(), any());
    }

    @Test
    void obtenerInformesRetornaResumenCorrecto() throws Exception {
        Map<String, Object> mockResumen = new HashMap<>();
        mockResumen.put("modulo", "Informes y Gestión Odontológica");
        mockResumen.put("estado", "DISPONIBLE");

        when(reportService.obtenerResumenInformes()).thenReturn(mockResumen);

        mockMvc.perform(get("/api/reports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modulo").value("Informes y Gestión Odontológica"))
                .andExpect(jsonPath("$.estado").value("DISPONIBLE"));
    }

    @Test
    void obtenerReporteInventarioRetornaListaDeInsumos() throws Exception {
        when(reportService.obtenerReporteInventario()).thenReturn(Arrays.asList(
                new InventarioItemDto(1L, "Anestesia", "Farmacología", 100, "ampollas", 10, "DISPONIBLE")
        ));

        mockMvc.perform(get("/api/reports/inventario"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Anestesia"))
                .andExpect(jsonPath("$[0].cantidadDisponible").value(100));
    }

    @Test
    void obtenerReporteAtencionesRetornaDetalle() throws Exception {
        Map<String, Object> mockReporte = new HashMap<>();
        mockReporte.put("tipo", "Turnos registrados");
        mockReporte.put("totalTurnos", 10);

        when(reportService.obtenerReporteAtenciones(null, null)).thenReturn(mockReporte);

        mockMvc.perform(get("/api/reports/atenciones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("Turnos registrados"))
                .andExpect(jsonPath("$.totalTurnos").value(10));
    }

    @Test
    void obtenerReporteAnualRetornaDatosDelAnioSolicitado() throws Exception {
        Map<String, Object> mockReporte = new HashMap<>();
        mockReporte.put("tipo", "Resumen anual de turnos registrados");
        mockReporte.put("anio", 2026);
        mockReporte.put("totalTurnos", 0);
        mockReporte.put("reportesFinancierosDisponibles", true);
        when(reportService.obtenerReporteAnual(2026)).thenReturn(mockReporte);

        mockMvc.perform(get("/api/reports/anual").param("anio", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.anio").value(2026))
                .andExpect(jsonPath("$.totalTurnos").value(0))
                .andExpect(jsonPath("$.reportesFinancierosDisponibles").value(true));
    }

    @Test
    void obtenerReporteAnualRechazaAniosFueraDeRango() throws Exception {
        mockMvc.perform(get("/api/reports/anual").param("anio", "0"))
                .andExpect(status().isBadRequest());
    }
}

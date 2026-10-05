package com.proyectohoussay.odonto.report;

import com.proyectohoussay.odonto.dto.InventarioItemDto;
import com.proyectohoussay.odonto.service.ReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReportController.class)
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReportService reportService;

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
                new InventarioItemDto(1L, "Anestesia", "Farmacología", 100, "ampollas", "DISPONIBLE")
        ));

        mockMvc.perform(get("/api/reports/inventario"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Anestesia"))
                .andExpect(jsonPath("$[0].cantidadDisponible").value(100));
    }

    @Test
    void obtenerReporteAtencionesRetornaDetalle() throws Exception {
        Map<String, Object> mockReporte = new HashMap<>();
        mockReporte.put("tipo", "Reporte de Atenciones");
        mockReporte.put("totalAtenciones", 10);

        when(reportService.obtenerReporteAtenciones()).thenReturn(mockReporte);

        mockMvc.perform(get("/api/reports/atenciones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("Reporte de Atenciones"))
                .andExpect(jsonPath("$.totalAtenciones").value(10));
    }
}

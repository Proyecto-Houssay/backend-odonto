package com.proyectohoussay.odonto.report;

import com.proyectohoussay.odonto.model.Insumo;
import com.proyectohoussay.odonto.repository.InsumoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class ReportPersistenceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InsumoRepository insumoRepository;

    @BeforeEach
    void setUp() {
        insumoRepository.deleteAll();
        insumoRepository.save(new Insumo("Fresas de diamante", "Instrumental", 40, "unidades", 10));
        insumoRepository.save(new Insumo("Baberos descartables", "Descartables", 5, "paquetes", 10));
    }

    @Test
    void getInventarioRetornaDatosPersistidosReales() throws Exception {
        mockMvc.perform(get("/api/reports/inventario")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].nombre").value("Fresas de diamante"))
                .andExpect(jsonPath("$[0].cantidadDisponible").value(40))
                .andExpect(jsonPath("$[0].estado").value("DISPONIBLE"))
                .andExpect(jsonPath("$[1].nombre").value("Baberos descartables"))
                .andExpect(jsonPath("$[1].cantidadDisponible").value(5))
                .andExpect(jsonPath("$[1].estado").value("BAJO_STOCK"));
    }

    @Test
    void getInventarioVacioRetornaListaVacia() throws Exception {
        insumoRepository.deleteAll();

        mockMvc.perform(get("/api/reports/inventario")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getResumenReflejaCantidadDeItemsPersistidos() throws Exception {
        mockMvc.perform(get("/api/reports")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modulo").value("Informes y Gestión Odontológica"))
                .andExpect(jsonPath("$.totalItemsInventario").value(2));
    }
}

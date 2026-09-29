package com.proyectohoussay.odonto;

import com.proyectohoussay.odonto.controller.TurnoController;
import com.proyectohoussay.odonto.model.Turno;
import com.proyectohoussay.odonto.service.TurnoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TurnoController.class)
public class TurnoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TurnoService turnoService;

    @Test
    void testListarTurnosReturnsOk() throws Exception {
        given(turnoService.listarTurnos()).willReturn(Collections.emptyList());

        mockMvc.perform(get("/api/turnos"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    void testComprobarDisponibilidadReturnsTrue() throws Exception {
        given(turnoService.comprobarDisponibilidad(1L, LocalDate.of(2026, 10, 1), LocalTime.of(9, 0)))
                .willReturn(true);

        mockMvc.perform(get("/api/turnos/disponibilidad")
                        .param("odontologoId", "1")
                        .param("fecha", "2026-10-01")
                        .param("hora", "09:00:00"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void testCrearTurnoReturns21Created() throws Exception {
        Turno turnoGuardado = new Turno(LocalDate.of(2026, 10, 1), LocalTime.of(9, 0), "Consulta general", "PENDIENTE", null, null);
        turnoGuardado.setId(1L);

        given(turnoService.crearTurno(any(Turno.class))).willReturn(turnoGuardado);

        String jsonBody = "{\"fecha\":\"2026-10-01\",\"hora\":\"09:00:00\",\"motivo\":\"Consulta general\",\"estado\":\"PENDIENTE\"}";

        mockMvc.perform(post("/api/turnos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.motivo").value("Consulta general"));
    }
}

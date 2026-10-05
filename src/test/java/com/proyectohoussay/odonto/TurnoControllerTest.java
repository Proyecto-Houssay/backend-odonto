package com.proyectohoussay.odonto;

import com.proyectohoussay.odonto.controller.TurnoController;
import com.proyectohoussay.odonto.dto.TurnoRequest;
import com.proyectohoussay.odonto.model.Turno;
import com.proyectohoussay.odonto.service.TurnoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TurnoController.class)
public class TurnoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
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

        given(turnoService.crearTurno(any(TurnoRequest.class))).willReturn(turnoGuardado);

        String jsonBody = "{\"fecha\":\"2026-10-01\",\"hora\":\"09:00:00\",\"motivo\":\"Consulta general\",\"estado\":\"PENDIENTE\",\"pacienteId\":1,\"odontologoId\":1}";

        mockMvc.perform(post("/api/turnos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.motivo").value("Consulta general"));
    }

    @Test
    void testCrearTurnoSinCamposObligatoriosReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/turnos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verify(turnoService, never()).crearTurno(any(TurnoRequest.class));
    }
}

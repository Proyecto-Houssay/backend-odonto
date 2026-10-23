package com.proyectohoussay.odonto;

import com.proyectohoussay.odonto.controller.TurnoController;
import com.proyectohoussay.odonto.dto.TurnoRequest;
import com.proyectohoussay.odonto.model.Turno;
import com.proyectohoussay.odonto.service.TurnoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
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
@AutoConfigureMockMvc(addFilters = false)
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
        LocalDate fecha = LocalDate.now().plusDays(1);
        given(turnoService.comprobarDisponibilidad(1L, fecha, LocalTime.of(9, 0)))
                .willReturn(true);

        mockMvc.perform(get("/api/turnos/disponibilidad")
                        .param("odontologoId", "1")
                        .param("fecha", fecha.toString())
                        .param("hora", "09:00:00"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void testCrearTurnoReturns21Created() throws Exception {
        LocalDate fecha = LocalDate.now().plusDays(1);
        Turno turnoGuardado = new Turno(fecha, LocalTime.of(9, 0), "Consulta general", "PENDIENTE", null, null);
        turnoGuardado.setId(1L);

        given(turnoService.crearTurno(any(TurnoRequest.class))).willReturn(turnoGuardado);

        String jsonBody = """
                {
                  "fecha": "%s",
                  "hora": "09:00:00",
                  "motivo": "Consulta general",
                  "estado": "PENDIENTE",
                  "pacienteId": 1,
                  "odontologoId": 1
                }
                """.formatted(fecha);

        mockMvc.perform(post("/api/turnos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.motivo").value("Consulta general"))
                .andExpect(jsonPath("$.mensaje").value("Turno registrado con éxito"));
    }

    @Test
    void testCrearTurnoConFechaPasadaReturnsBadRequest() throws Exception {
        String fechaPasada = LocalDate.now().minusDays(1).toString();
        String jsonBody = """
                {
                  "fecha": "%s",
                  "hora": "09:00:00",
                  "motivo": "Consulta general",
                  "pacienteId": 1,
                  "odontologoId": 1
                }
                """.formatted(fechaPasada);

        mockMvc.perform(post("/api/turnos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("La fecha del turno no puede ser anterior a la fecha actual."));

        verify(turnoService, never()).crearTurno(any(TurnoRequest.class));
    }

    @Test
    void testCrearTurnoAceptaFechaActual() throws Exception {
        LocalDate fecha = LocalDate.now();
        Turno turnoGuardado = new Turno(fecha, LocalTime.of(9, 0), "Consulta general", "PENDIENTE", null, null);
        turnoGuardado.setId(1L);
        given(turnoService.crearTurno(any(TurnoRequest.class))).willReturn(turnoGuardado);

        String jsonBody = """
                {
                  "fecha": "%s",
                  "hora": "09:00:00",
                  "motivo": "Consulta general",
                  "pacienteId": 1,
                  "odontologoId": 1
                }
                """.formatted(fecha);

        mockMvc.perform(post("/api/turnos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isCreated());

        verify(turnoService).crearTurno(any(TurnoRequest.class));
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

package com.proyectohoussay.odonto.controller;

import com.proyectohoussay.odonto.dto.PersonalRequest;
import com.proyectohoussay.odonto.model.CargoPersonal;
import com.proyectohoussay.odonto.model.Personal;
import com.proyectohoussay.odonto.service.PersonalService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PersonalController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("PersonalController")
class PersonalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PersonalService personalService;

    @Test
    @DisplayName("POST /api/personal returns 201 Created on valid request")
    void postCreatesPersonal() throws Exception {
        Personal personal = new Personal(
                "Carlos", "Vega", "34567890", "carlos@novadent.com", "1144556677",
                CargoPersonal.RECEPCIONISTA, LocalDate.now(), "Mañana", 5L
        );
        personal.setId(1L);

        when(personalService.crearPersonal(any(PersonalRequest.class))).thenReturn(personal);

        mockMvc.perform(post("/api/personal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "Carlos",
                                  "apellido": "Vega",
                                  "dni": "34567890",
                                  "email": "carlos@novadent.com",
                                  "telefono": "1144556677",
                                  "cargo": "RECEPCIONISTA",
                                  "horarioLaboral": "Mañana",
                                  "usuarioId": 5
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombre").value("Carlos"))
                .andExpect(jsonPath("$.dni").value("34567890"))
                .andExpect(jsonPath("$.cargo").value("RECEPCIONISTA"))
                .andExpect(jsonPath("$.usuarioId").value(5));

        verify(personalService).crearPersonal(any(PersonalRequest.class));
    }

    @Test
    @DisplayName("GET /api/personal lists all staff")
    void getListarPersonalReturnsList() throws Exception {
        Personal p = new Personal(
                "Carlos", "Vega", "34567890", "carlos@novadent.com", null,
                CargoPersonal.RECEPCIONISTA, LocalDate.now(), null, null
        );
        p.setId(1L);

        when(personalService.listarPersonal(null, null)).thenReturn(List.of(p));

        mockMvc.perform(get("/api/personal"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].dni").value("34567890"));
    }

    @Test
    @DisplayName("GET /api/personal/{id} returns personal details")
    void getByIdReturnsPersonal() throws Exception {
        Personal p = new Personal(
                "Carlos", "Vega", "34567890", "carlos@novadent.com", null,
                CargoPersonal.RECEPCIONISTA, LocalDate.now(), null, null
        );
        p.setId(1L);

        when(personalService.obtenerPorId(1L)).thenReturn(p);

        mockMvc.perform(get("/api/personal/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombre").value("Carlos"));
    }

    @Test
    @DisplayName("PUT /api/personal/{id} updates personal")
    void putUpdatesPersonal() throws Exception {
        Personal p = new Personal(
                "Carlos", "Vega", "34567890", "carlos@novadent.com", null,
                CargoPersonal.SECRETARIO, LocalDate.now(), "Tarde", null
        );
        p.setId(1L);

        when(personalService.actualizarPersonal(eq(1L), any(PersonalRequest.class))).thenReturn(p);

        mockMvc.perform(put("/api/personal/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "Carlos",
                                  "apellido": "Vega",
                                  "dni": "34567890",
                                  "cargo": "SECRETARIO",
                                  "horarioLaboral": "Tarde"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cargo").value("SECRETARIO"));
    }

    @Test
    @DisplayName("DELETE /api/personal/{id} soft deletes and returns 204 No Content")
    void deleteSoftDeletesPersonal() throws Exception {
        mockMvc.perform(delete("/api/personal/1"))
                .andExpect(status().isNoContent());

        verify(personalService).darDeBaja(1L);
    }
}

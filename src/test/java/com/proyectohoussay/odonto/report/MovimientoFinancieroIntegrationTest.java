package com.proyectohoussay.odonto.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyectohoussay.odonto.auth.JwtTokenService;
import com.proyectohoussay.odonto.model.Especialidad;
import com.proyectohoussay.odonto.model.MovimientoFinanciero;
import com.proyectohoussay.odonto.model.Odontologo;
import com.proyectohoussay.odonto.model.Turno;
import com.proyectohoussay.odonto.patient.Paciente;
import com.proyectohoussay.odonto.patient.PacienteRepository;
import com.proyectohoussay.odonto.repository.EspecialidadRepository;
import com.proyectohoussay.odonto.repository.MovimientoFinancieroRepository;
import com.proyectohoussay.odonto.repository.OdontologoRepository;
import com.proyectohoussay.odonto.repository.TurnoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.auth.jwt-secret=finance-test-key-with-at-least-thirty-two-bytes")
@AutoConfigureMockMvc
class MovimientoFinancieroIntegrationTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtTokenService jwtTokenService;
    @Autowired private MovimientoFinancieroRepository movimientoRepository;
    @Autowired private TurnoRepository turnoRepository;
    @Autowired private PacienteRepository pacienteRepository;
    @Autowired private OdontologoRepository odontologoRepository;
    @Autowired private EspecialidadRepository especialidadRepository;

    private Turno turno;

    @BeforeEach
    void setUp() {
        movimientoRepository.deleteAll();
        turnoRepository.deleteAll();
        odontologoRepository.deleteAll();
        pacienteRepository.deleteAll();
        especialidadRepository.deleteAll();

        Paciente paciente = new Paciente();
        paciente.setNombre("Ana");
        paciente.setApellido("Lopez");
        paciente.setDni("30111222");
        paciente.setEmail("ana.finance@example.com");
        paciente.setFechaNacimiento(LocalDate.of(1990, 1, 1));
        paciente = pacienteRepository.save(paciente);

        Especialidad especialidad = especialidadRepository.save(new Especialidad("Clínica general", "Atención general"));
        Odontologo odontologo = odontologoRepository.save(new Odontologo(
                "Laura", "Rios", "MN-FIN-1", "laura.finance@example.com", "1144556677", especialidad));
        turno = turnoRepository.save(new Turno(LocalDate.now(), LocalTime.of(9, 30), "Consulta", "COMPLETADO",
                paciente, odontologo));
    }

    @Test
    void cobrosYResultadoSeCalculanDesdeMovimientosPersistidos() throws Exception {
        LocalDate today = LocalDate.now();
        turnoRepository.save(new Turno(today, LocalTime.of(10, 30), "Control", "PENDIENTE",
                turno.getPaciente(), turno.getOdontologo()));
        postMovement(Map.of("tipo", "INGRESO", "fecha", today.toString(), "importe", "12500.00",
                "concepto", "Consulta odontológica", "medioPago", "TARJETA", "turnoId", turno.getId()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("INGRESO"));
        postMovement(Map.of("tipo", "EGRESO", "fecha", today.toString(), "importe", "2500.00",
                "concepto", "Compra de materiales", "medioPago", "TRANSFERENCIA"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("EGRESO"));

        mockMvc.perform(get("/api/reports/cobros")
                        .header("Authorization", adminAuthorization())
                        .param("desde", today.toString())
                        .param("hasta", today.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].importe").value(12500.0))
                .andExpect(jsonPath("$[0].turnoId").value(turno.getId()));

        mockMvc.perform(get("/api/reports/ganancias")
                        .header("Authorization", adminAuthorization())
                        .param("desde", today.toString())
                        .param("hasta", today.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ingresosRegistrados").value(12500.0))
                .andExpect(jsonPath("$.egresosRegistrados").value(2500.0))
                .andExpect(jsonPath("$.gananciaNetaRegistrada").value(10000.0))
                .andExpect(jsonPath("$.cantidadMovimientos").value(2))
                .andExpect(jsonPath("$.sinRegistros").value(false));

        mockMvc.perform(get("/api/reports/atenciones")
                        .header("Authorization", adminAuthorization())
                        .param("desde", today.toString())
                        .param("hasta", today.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTurnos").value(2))
                .andExpect(jsonPath("$.totalAtenciones").value(1));

        assertThat(movimientoRepository.findByFechaBetween(today, today)).hasSize(2);
        assertThat(movimientoRepository.findByFechaBetween(today, today).stream()
                .map(MovimientoFinanciero::getImporte).reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo("15000.00");
    }

    @Test
    void rechazaCobroSinTurnoYNoGuardaDatosInventados() throws Exception {
        LocalDate today = LocalDate.now();
        postMovement(Map.of("tipo", "INGRESO", "fecha", today.toString(), "importe", "1000.00",
                "concepto", "Cobro sin turno"))
                .andExpect(status().isBadRequest());

        assertThat(movimientoRepository.count()).isZero();
    }

    private org.springframework.test.web.servlet.ResultActions postMovement(Map<String, Object> body) throws Exception {
        return mockMvc.perform(post("/api/finanzas/movimientos")
                .header("Authorization", adminAuthorization())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    private String adminAuthorization() {
        return "Bearer " + jwtTokenService.issue("finance-admin", "ADMIN");
    }
}

package com.proyectohoussay.odonto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyectohoussay.odonto.model.Especialidad;
import com.proyectohoussay.odonto.model.Odontologo;
import com.proyectohoussay.odonto.patient.Paciente;
import com.proyectohoussay.odonto.patient.PacienteRepository;
import com.proyectohoussay.odonto.repository.EspecialidadRepository;
import com.proyectohoussay.odonto.repository.OdontologoRepository;
import com.proyectohoussay.odonto.repository.TurnoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TurnoPersistenceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PacienteRepository pacienteRepository;

    @Autowired
    private EspecialidadRepository especialidadRepository;

    @Autowired
    private OdontologoRepository odontologoRepository;

    @Autowired
    private TurnoRepository turnoRepository;

    private Paciente paciente;
    private Odontologo odontologo;

    @BeforeEach
    void prepararEntidades() {
        turnoRepository.deleteAll();
        odontologoRepository.deleteAll();
        pacienteRepository.deleteAll();
        especialidadRepository.deleteAll();

        paciente = new Paciente();
        paciente.setNombre("Ana");
        paciente.setApellido("Lopez");
        paciente.setDni("30111222");
        paciente.setEmail("ana.lopez@example.com");
        paciente.setFechaNacimiento(LocalDate.of(1990, 1, 1));
        paciente = pacienteRepository.save(paciente);

        Especialidad especialidad = especialidadRepository.save(new Especialidad("Ortodoncia", "Alineación dental"));
        odontologo = odontologoRepository.save(
                new Odontologo("Laura", "Rios", "MN-9999", "laura@odonto.com", "1188776655", especialidad));
    }

    @Test
    void registraTurnoPorApiYPersistePacienteOdontologoFechaHorarioYMotivo() throws Exception {
        Map<String, Object> body = Map.of(
                "fecha", LocalDate.now().plusDays(1).toString(),
                "hora", "10:30:00",
                "motivo", "Limpieza dental",
                "pacienteId", paciente.getId(),
                "odontologoId", odontologo.getId());

        mockMvc.perform(post("/api/turnos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.paciente.id").value(paciente.getId()))
                .andExpect(jsonPath("$.odontologo.id").value(odontologo.getId()))
                .andExpect(jsonPath("$.motivo").value("Limpieza dental"))
                .andExpect(jsonPath("$.mensaje").value("Turno registrado con éxito"));

        assertThat(turnoRepository.count()).isEqualTo(1);
        var turnoGuardado = turnoRepository.findAll().get(0);
        assertThat(turnoGuardado.getFecha()).isEqualTo(LocalDate.now().plusDays(1));
        assertThat(turnoGuardado.getHora()).hasToString("10:30");
        assertThat(turnoGuardado.getPaciente().getId()).isEqualTo(paciente.getId());
        assertThat(turnoGuardado.getOdontologo().getId()).isEqualTo(odontologo.getId());
    }

    @Test
    void noPersisteTurnoConReferenciasInexistentes() throws Exception {
        Map<String, Object> body = Map.of(
                "fecha", LocalDate.now().plusDays(1).toString(),
                "hora", "10:30:00",
                "motivo", "Limpieza dental",
                "pacienteId", 99999,
                "odontologoId", odontologo.getId());

        mockMvc.perform(post("/api/turnos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());

        assertThat(turnoRepository.count()).isZero();
    }

    @Test
    void rechazaOdontologoInexistente() throws Exception {
        Map<String, Object> body = Map.of(
                "fecha", LocalDate.now().plusDays(1).toString(),
                "hora", "10:30:00",
                "motivo", "Limpieza dental",
                "pacienteId", paciente.getId(),
                "odontologoId", 99999);

        mockMvc.perform(post("/api/turnos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());

        assertThat(turnoRepository.count()).isZero();
    }

    @Test
    void rechazaHorarioOcupadoParaElMismoOdontologo() throws Exception {
        LocalDate fecha = LocalDate.now().plusDays(1);
        Map<String, Object> body = Map.of(
                "fecha", fecha.toString(),
                "hora", "10:30:00",
                "motivo", "Limpieza dental",
                "pacienteId", paciente.getId(),
                "odontologoId", odontologo.getId());
        String json = objectMapper.writeValueAsString(body);

        mockMvc.perform(post("/api/turnos").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/turnos").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isConflict());

        assertThat(turnoRepository.count()).isEqualTo(1);
    }

    @Test
    void noPersisteTurnoCuandoFaltanCamposObligatorios() throws Exception {
        mockMvc.perform(post("/api/turnos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        assertThat(turnoRepository.count()).isZero();
    }

    @Test
    void rechazaTurnoConFechaPasada() throws Exception {
        Map<String, Object> body = Map.of(
                "fecha", LocalDate.now().minusDays(1).toString(),
                "hora", "10:30:00",
                "motivo", "Consulta vencida",
                "pacienteId", paciente.getId(),
                "odontologoId", odontologo.getId());

        mockMvc.perform(post("/api/turnos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());

        assertThat(turnoRepository.count()).isZero();
    }
}

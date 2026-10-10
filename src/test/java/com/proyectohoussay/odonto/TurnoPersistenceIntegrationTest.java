package com.proyectohoussay.odonto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyectohoussay.odonto.model.Especialidad;
import com.proyectohoussay.odonto.model.Odontologo;
import com.proyectohoussay.odonto.model.Turno;
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
import java.time.LocalTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
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

    @Autowired
    private com.proyectohoussay.odonto.service.TurnoService turnoService;

    private Paciente paciente;
    private Odontologo odontologo;
    private Especialidad especialidad;

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

        especialidad = especialidadRepository.save(new Especialidad("Ortodoncia", "Alineación dental"));
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

    @Test
    void actualizarTurnoRechazaFechaPasadaYNoModificaElTurno() throws Exception {
        LocalDate fechaOriginal = LocalDate.now().plusDays(2);
        Turno turno = persistirTurno(fechaOriginal, LocalTime.of(9, 0), odontologo, "PENDIENTE");
        Map<String, Object> body = updateBody(LocalDate.now().minusDays(1), LocalTime.of(9, 0), odontologo);

        mockMvc.perform(put("/api/turnos/{id}", turno.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());

        assertThat(turnoRepository.findById(turno.getId()).orElseThrow().getFecha()).isEqualTo(fechaOriginal);
    }

    @Test
    void actualizarTurnoRechazaHorarioOcupadoPorOtroTurnoActivo() throws Exception {
        LocalDate fecha = LocalDate.now().plusDays(3);
        LocalTime horaOcupada = LocalTime.of(10, 0);
        Turno ocupante = persistirTurno(fecha, horaOcupada, odontologo, "PENDIENTE");
        Turno aActualizar = persistirTurno(fecha, LocalTime.of(11, 0), odontologo, "PENDIENTE");

        mockMvc.perform(put("/api/turnos/{id}", aActualizar.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateBody(fecha, horaOcupada, odontologo))))
                .andExpect(status().isConflict());

        assertThat(turnoRepository.findById(aActualizar.getId()).orElseThrow().getHora())
                .isEqualTo(LocalTime.of(11, 0));
        assertThat(turnoRepository.findById(ocupante.getId())).isPresent();
    }

    @Test
    void actualizarTurnoPermiteConservarSuPropioHorario() throws Exception {
        LocalDate fecha = LocalDate.now().plusDays(3);
        Turno turno = persistirTurno(fecha, LocalTime.of(10, 0), odontologo, "PENDIENTE");

        mockMvc.perform(put("/api/turnos/{id}", turno.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateBody(fecha, LocalTime.of(10, 0), odontologo))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(turno.getId()))
                .andExpect(jsonPath("$.odontologo.especialidad.odontologos").doesNotExist());

        assertThat(turnoRepository.count()).isEqualTo(1);
    }

    @Test
    void actualizarTurnoVerificaDisponibilidadDelNuevoOdontologo() throws Exception {
        LocalDate fecha = LocalDate.now().plusDays(4);
        LocalTime hora = LocalTime.of(11, 30);
        Turno turno = persistirTurno(fecha, hora, odontologo, "PENDIENTE");
        Odontologo nuevoOdontologo = odontologoRepository.save(
                new Odontologo("Mario", "Perez", "MN-8888", "mario@odonto.com", "1188776656", especialidad));

        mockMvc.perform(put("/api/turnos/{id}", turno.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateBody(fecha, hora, nuevoOdontologo))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.odontologo.id").value(nuevoOdontologo.getId()));

        assertThat(turnoRepository.findById(turno.getId()).orElseThrow().getOdontologo().getId())
                .isEqualTo(nuevoOdontologo.getId());
    }

    @Test
    void horarioCanceladoSeLiberaParaActualizarOtroTurno() throws Exception {
        LocalDate fecha = LocalDate.now().plusDays(5);
        LocalTime hora = LocalTime.of(12, 0);
        Turno cancelado = persistirTurno(fecha, hora, odontologo, "PENDIENTE");
        Turno aActualizar = persistirTurno(fecha, LocalTime.of(13, 0), odontologo, "PENDIENTE");

        mockMvc.perform(delete("/api/turnos/{id}", cancelado.getId()))
                .andExpect(status().isNoContent());
        assertThat(turnoRepository.findById(cancelado.getId()).orElseThrow().getEstado()).isEqualTo("CANCELADO");

        mockMvc.perform(put("/api/turnos/{id}", aActualizar.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateBody(fecha, hora, odontologo))))
                .andExpect(status().isOk());

        assertThat(turnoRepository.findById(aActualizar.getId()).orElseThrow().getHora()).isEqualTo(hora);
        assertThat(turnoService.comprobarDisponibilidad(odontologo.getId(), fecha, LocalTime.of(13, 0))).isTrue();
    }

    @Test
    void rutasDeConsultaDeTurnosRespondenConResumenesSinRelacionesRecursivas() throws Exception {
        LocalDate fecha = LocalDate.now().plusDays(2);
        Turno turno = persistirTurno(fecha, LocalTime.of(10, 0), odontologo, "PENDIENTE");

        mockMvc.perform(get("/api/turnos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].odontologo.especialidad.odontologos").doesNotExist());
        mockMvc.perform(get("/api/turnos/{id}", turno.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paciente.id").value(paciente.getId()))
                .andExpect(jsonPath("$.odontologo.especialidad.nombre").value("Ortodoncia"))
                .andExpect(jsonPath("$.odontologo.especialidad.odontologos").doesNotExist());
        mockMvc.perform(get("/api/turnos/fecha/{fecha}", fecha))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(turno.getId()));
        mockMvc.perform(get("/api/turnos/odontologo/{odontologoId}", odontologo.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(turno.getId()));
        mockMvc.perform(get("/api/turnos/paciente/{pacienteId}", paciente.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(turno.getId()));
    }

    private Turno persistirTurno(LocalDate fecha, java.time.LocalTime hora, Odontologo profesional, String estado) {
        return turnoRepository.save(new Turno(fecha, hora, "Control", estado, paciente, profesional));
    }

    private Map<String, Object> updateBody(LocalDate fecha, LocalTime hora, Odontologo profesional) {
        return Map.of(
                "fecha", fecha.toString(),
                "hora", hora.toString(),
                "motivo", "Control actualizado",
                "estado", "PENDIENTE",
                "pacienteId", paciente.getId(),
                "odontologoId", profesional.getId());
    }
}

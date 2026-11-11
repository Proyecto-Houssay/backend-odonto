package com.proyectohoussay.odonto.chatbot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyectohoussay.odonto.auth.JwtTokenService;
import com.proyectohoussay.odonto.model.Especialidad;
import com.proyectohoussay.odonto.model.Odontologo;
import com.proyectohoussay.odonto.model.Turno;
import com.proyectohoussay.odonto.model.Usuario;
import com.proyectohoussay.odonto.patient.Paciente;
import com.proyectohoussay.odonto.patient.PacienteRepository;
import com.proyectohoussay.odonto.repository.EspecialidadRepository;
import com.proyectohoussay.odonto.repository.OdontologoRepository;
import com.proyectohoussay.odonto.repository.TurnoRepository;
import com.proyectohoussay.odonto.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ChatbotAgendaIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JwtTokenService tokens;
    @Autowired UsuarioRepository usuarios;
    @Autowired PacienteRepository pacientes;
    @Autowired EspecialidadRepository especialidades;
    @Autowired OdontologoRepository odontologos;
    @Autowired TurnoRepository turnos;

    private String bearer(String username, String role) {
        Usuario user = usuarios.saveAndFlush(new Usuario(
                username, "Ana", "Nieto", username + "@example.com", "temp-pass", role, true));
        return "Bearer " + tokens.issueToken(user).token();
    }

    private Paciente crearFicha(String username, String dni) {
        Usuario user = usuarios.findByUsername(username).orElseThrow();
        Paciente paciente = new Paciente();
        paciente.setNombre(user.getNombre());
        paciente.setApellido(user.getApellido());
        paciente.setEmail(user.getEmail());
        paciente.setDni(dni);
        paciente.setFechaNacimiento(LocalDate.of(1995, 5, 10));
        pacientes.saveAndFlush(paciente);
        user.setPaciente(paciente);
        usuarios.saveAndFlush(user);
        return paciente;
    }

    private LocalDate nextBusinessDay() {
        LocalDate date = LocalDate.now().plusDays(1);
        while (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            date = date.plusDays(1);
        }
        return date;
    }

    private Odontologo createDentist(String nombre, String apellido, String matricula, String especialidadNombre, boolean activo) {
        Especialidad esp = especialidades.saveAndFlush(new Especialidad(especialidadNombre, "Especialidad " + especialidadNombre));
        Odontologo doc = new Odontologo(nombre, apellido, matricula, apellido.toLowerCase() + "@novadent.test", "1144445555", esp);
        doc.setActivo(activo);
        return odontologos.saveAndFlush(doc);
    }

    @Test
    void preguntaPorDoctoresDisponiblesListaSoloActivosYProximosHorarios() throws Exception {
        String patient = bearer("chat-doc-patient", "PACIENTE");
        createDentist("Laura", "Pérez", "MN-101", "Ortodoncia", true);
        createDentist("Carlos", "Inactivo", "MN-102", "Cirugía", false);

        mvc.perform(post("/api/chatbot/ask")
                        .header("Authorization", patient)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mensaje\":\"¿Qué doctores disponibles hay?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.intencion").value("doctores"))
                .andExpect(jsonPath("$.respuesta", containsString("Laura Pérez")))
                .andExpect(jsonPath("$.respuesta", containsString("Ortodoncia")))
                .andExpect(jsonPath("$.respuesta", not(containsString("Inactivo"))))
                .andExpect(jsonPath("$.agenda.doctores.length()").value(1))
                .andExpect(jsonPath("$.agenda.doctores[0].nombreCompleto").value("Laura Pérez"))
                .andExpect(jsonPath("$.agenda.slotsDisponibles").isNotEmpty());
    }

    @Test
    void preguntaPorTurnosDisponiblesRespondeConHorariosRealesYExcluyeOcupados() throws Exception {
        String patient = bearer("chat-slots-patient", "PACIENTE");
        crearFicha("chat-slots-patient", "33111222");
        Odontologo dentist = createDentist("Martín", "Sánchez", "MN-201", "Odontología General", true);
        Paciente otroPaciente = new Paciente();
        otroPaciente.setNombre("Otro");
        otroPaciente.setApellido("Paciente");
        otroPaciente.setEmail("otro@example.com");
        otroPaciente.setDni("99888777");
        otroPaciente.setFechaNacimiento(LocalDate.of(1990, 1, 1));
        otroPaciente = pacientes.saveAndFlush(otroPaciente);

        LocalDate dia = nextBusinessDay();
        turnos.saveAndFlush(new Turno(dia, LocalTime.of(9, 0), "Control", "PENDIENTE", otroPaciente, dentist));

        mvc.perform(post("/api/chatbot/ask")
                        .header("Authorization", patient)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mensaje\":\"Hola, ¿qué turnos tienen disponibles el " + dia + "?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.intencion").value("turno"))
                .andExpect(jsonPath("$.respuesta", containsString("¡Hola! Sí, mirá, tenemos estos turnos disponibles")))
                .andExpect(jsonPath("$.agenda.requiereFichaPaciente").value(false))
                .andExpect(jsonPath("$.agenda.slotsDisponibles[*].etiquetaHora", not(hasItem("09:00"))))
                .andExpect(jsonPath("$.agenda.slotsDisponibles[*].etiquetaHora", hasItem("09:30")));
    }

    @Test
    void pacientePuedeReservarTurnoDesdeChatbotYApareceEnTurnosYMisTurnos() throws Exception {
        String patient = bearer("chat-book-patient", "PACIENTE");
        Paciente propia = crearFicha("chat-book-patient", "40123456");
        String admin = bearer("chat-book-admin", "ADMINISTRADOR");
        Odontologo dentist = createDentist("Lucía", "Gómez", "MN-301", "Endodoncia", true);
        LocalDate dia = nextBusinessDay();

        // Incluso si el cliente intentara mandar otro pacienteId, el backend fuerza el del JWT del paciente.
        String payload = """
                {"odontologoId":%d,"fecha":"%s","hora":"10:30:00","motivo":"Limpieza dental","pacienteId":999999}
                """.formatted(dentist.getId(), dia);

        mvc.perform(post("/api/chatbot/turnos/reservar")
                        .header("Authorization", patient)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pacienteId").value(propia.getId()))
                .andExpect(jsonPath("$.odontologoNombre").value("Lucía Gómez"))
                .andExpect(jsonPath("$.motivo").value("Limpieza dental"))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"));

        // Verificamos que aparece en "Mis turnos" del paciente
        mvc.perform(get("/api/portal-paciente/turnos").header("Authorization", patient))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].motivo").value("Limpieza dental"))
                .andExpect(jsonPath("$[0].odontologoNombre").value("Lucía Gómez"));

        // Verificamos que aparece en el módulo general de "Turnos" del equipo
        mvc.perform(get("/api/turnos").header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.motivo=='Limpieza dental')].odontologo.apellido").value("Gómez"));

        // Reintentar reservar exactamente la misma franja devuelve 409 Conflict
        mvc.perform(post("/api/chatbot/turnos/reservar")
                        .header("Authorization", patient)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict());
    }

    @Test
    void pacientePuedeReservarPorTextoConversacionalDirectoEnAsk() throws Exception {
        String patient = bearer("chat-nlp-patient", "PACIENTE");
        crearFicha("chat-nlp-patient", "42555666");
        Odontologo dentist = createDentist("Sofía", "Martínez", "MN-401", "Periodoncia", true);
        LocalDate dia = nextBusinessDay();

        mvc.perform(post("/api/chatbot/ask")
                        .header("Authorization", patient)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mensaje\":\"Quiero sacar un turno el " + dia + " a las 11:00 con Martínez por control general\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.intencion").value("turno"))
                .andExpect(jsonPath("$.agenda.turnoConfirmado.motivo").value("Control general"))
                .andExpect(jsonPath("$.agenda.turnoConfirmado.odontologoNombre").value("Sofía Martínez"));

        assertThat(turnos.findByFechaAndOdontologoId(dia, dentist.getId())).hasSize(1);
    }

    @Test
    void pacienteSinFichaRecibeConflictAlIntentarReservarYSeguridadRechazaNoAutorizados() throws Exception {
        String patientSinFicha = bearer("chat-no-ficha", "PACIENTE");
        String staffDentist = bearer("chat-staff-doc", "ODONTOLOGO");
        Odontologo dentist = createDentist("Diego", "Ruiz", "MN-501", "Implantología", true);
        LocalDate dia = nextBusinessDay();

        mvc.perform(post("/api/chatbot/ask")
                        .header("Authorization", patientSinFicha)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mensaje\":\"Quiero sacar un turno\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.agenda.requiereFichaPaciente").value(true));

        String payload = """
                {"odontologoId":%d,"fecha":"%s","hora":"10:00:00","motivo":"Control"}
                """.formatted(dentist.getId(), dia);

        mvc.perform(post("/api/chatbot/turnos/reservar")
                        .header("Authorization", patientSinFicha)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict());

        mvc.perform(get("/api/chatbot/doctores"))
                .andExpect(status().isUnauthorized());

        mvc.perform(get("/api/chatbot/doctores").header("Authorization", staffDentist))
                .andExpect(status().isForbidden());
    }
}

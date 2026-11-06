package com.proyectohoussay.odonto.teletriaje;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyectohoussay.odonto.auth.JwtTokenService;
import com.proyectohoussay.odonto.model.Odontologo;
import com.proyectohoussay.odonto.model.Usuario;
import com.proyectohoussay.odonto.patient.Paciente;
import com.proyectohoussay.odonto.patient.PacienteRepository;
import com.proyectohoussay.odonto.repository.OdontologoRepository;
import com.proyectohoussay.odonto.repository.TurnoRepository;
import com.proyectohoussay.odonto.repository.UsuarioRepository;
import com.proyectohoussay.odonto.teletriaje.dto.ReservarTeletriajeRequest;
import com.proyectohoussay.odonto.teletriaje.dto.SolicitudTeletriajeRequest;
import com.proyectohoussay.odonto.teletriaje.dto.ValidarPrioridadRequest;
import com.proyectohoussay.odonto.teletriaje.model.NivelPrioridad;
import com.proyectohoussay.odonto.teletriaje.model.SolicitudTeletriaje;
import com.proyectohoussay.odonto.teletriaje.repository.CitaTeletriajeRepository;
import com.proyectohoussay.odonto.teletriaje.repository.SolicitudTeletriajeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TeletriajeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PacienteRepository pacienteRepository;

    @Autowired
    private OdontologoRepository odontologoRepository;

    @Autowired
    private TurnoRepository turnoRepository;

    @Autowired
    private SolicitudTeletriajeRepository solicitudRepository;

    @Autowired
    private CitaTeletriajeRepository citaRepository;

    @Autowired
    private com.proyectohoussay.odonto.repository.EspecialidadRepository especialidadRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;
    private String odontologoToken;
    private String recepcionistaToken;
    private String paciente1Token;
    private String paciente2Token;

    private Paciente paciente1;
    private Paciente paciente2;
    private Odontologo odontologo;

    @BeforeEach
    void setUp() {
        citaRepository.deleteAll();
        solicitudRepository.deleteAll();
        turnoRepository.deleteAll();
        odontologoRepository.deleteAll();
        especialidadRepository.deleteAll();
        pacienteRepository.deleteAll();
        usuarioRepository.deleteAll();

        Usuario admin = new Usuario("admin", "Admin", "General", "admin@novadent.local", passwordEncoder.encode("pass"), "ADMINISTRADOR", true);
        Usuario odontoUser = new Usuario("dentist", "Dentist", "Pro", "dentist@novadent.local", passwordEncoder.encode("pass"), "ODONTOLOGO", true);
        Usuario recepUser = new Usuario("reception", "Recep", "Front", "recep@novadent.local", passwordEncoder.encode("pass"), "RECEPCIONISTA", true);
        Usuario pac1User = new Usuario("paciente1", "Juan", "Perez", "juan@paciente.local", passwordEncoder.encode("pass"), "PACIENTE", true);
        Usuario pac2User = new Usuario("paciente2", "Maria", "Lopez", "maria@paciente.local", passwordEncoder.encode("pass"), "PACIENTE", true);

        usuarioRepository.save(admin);
        usuarioRepository.save(odontoUser);
        usuarioRepository.save(recepUser);
        usuarioRepository.save(pac1User);
        usuarioRepository.save(pac2User);

        adminToken = "Bearer " + jwtTokenService.issueToken(admin).token();
        odontologoToken = "Bearer " + jwtTokenService.issueToken(odontoUser).token();
        recepcionistaToken = "Bearer " + jwtTokenService.issueToken(recepUser).token();
        paciente1Token = "Bearer " + jwtTokenService.issueToken(pac1User).token();
        paciente2Token = "Bearer " + jwtTokenService.issueToken(pac2User).token();

        paciente1 = new Paciente();
        paciente1.setNombre("Juan");
        paciente1.setApellido("Perez");
        paciente1.setDni("11223344");
        paciente1.setEmail("juan@paciente.local");
        paciente1.setFechaNacimiento(LocalDate.of(1990, 1, 1));
        paciente1 = pacienteRepository.save(paciente1);

        paciente2 = new Paciente();
        paciente2.setNombre("Maria");
        paciente2.setApellido("Lopez");
        paciente2.setDni("55667788");
        paciente2.setEmail("maria@paciente.local");
        paciente2.setFechaNacimiento(LocalDate.of(1995, 5, 5));
        paciente2 = pacienteRepository.save(paciente2);

        var esp = especialidadRepository.save(new com.proyectohoussay.odonto.model.Especialidad("Odontología General", "Atención primaria odontológica"));
        odontologo = new Odontologo("Dr. Lucas", "Vazquez", "MN-12345", "lucas@example.com", "11223344", esp);
        odontologo = odontologoRepository.save(odontologo);
    }

    @Test
    void endpointsSinAutenticacionRetornan401() throws Exception {
        mockMvc.perform(get("/api/teletriaje/solicitudes"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/teletriaje/solicitudes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void pacienteNoPuedeListarColaCompletaDeSolicitudesRetorna403() throws Exception {
        mockMvc.perform(get("/api/teletriaje/solicitudes")
                        .header("Authorization", paciente1Token))
                .andExpect(status().isForbidden());
    }

    @Test
    void recepcionistaYOdontologoPuedenListarColaRetorna200() throws Exception {
        mockMvc.perform(get("/api/teletriaje/solicitudes")
                        .header("Authorization", recepcionistaToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

        mockMvc.perform(get("/api/teletriaje/solicitudes")
                        .header("Authorization", odontologoToken))
                .andExpect(status().isOk());
    }

    @Test
    void flujoCompletoSolicitudValidacionReservaYConflicto409() throws Exception {
        // 1. Crear solicitud con signos de alarma
        var req = new SolicitudTeletriajeRequest(
                paciente1.getId(),
                "Hinchazón severa y fiebre alta",
                "Dolor en maxilar inferior irradiado a cuello",
                "AMARILLO",
                true,
                "WEB"
        );

        String createJson = objectMapper.writeValueAsString(req);

        var resultCreacion = mockMvc.perform(post("/api/teletriaje/solicitudes")
                        .header("Authorization", recepcionistaToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.signosAlarma").value(true))
                .andExpect(jsonPath("$.derivacionHumana").value(true))
                .andExpect(jsonPath("$.estado").value("DERIVADA_URGENTE"))
                .andExpect(jsonPath("$.estadoValidacion").value("SUGERIDA"))
                .andReturn();

        Long solicitudId = objectMapper.readTree(resultCreacion.getResponse().getContentAsString()).get("id").asLong();

        // 2. Profesional odontólogo valida prioridad a ROJO
        var valReq = new ValidarPrioridadRequest("ROJO", "Confirmado flemón con riesgo de celulitis facial");
        mockMvc.perform(put("/api/teletriaje/solicitudes/" + solicitudId + "/prioridad")
                        .header("Authorization", odontologoToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(valReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prioridadValidada").value("ROJO"))
                .andExpect(jsonPath("$.prioridadEfectiva").value("ROJO"))
                .andExpect(jsonPath("$.estadoValidacion").value("VALIDADA"))
                .andExpect(jsonPath("$.observacionesClinicas").value("Confirmado flemón con riesgo de celulitis facial"));

        // 3. Recepcionista reserva turno para la solicitud
        LocalDate fecha = LocalDate.now().plusDays(1);
        LocalTime hora = LocalTime.of(15, 30);
        var resReq = new ReservarTeletriajeRequest(odontologo.getId(), fecha, hora, "Atención urgente flemón", true, true);

        var resultReserva = mockMvc.perform(post("/api/teletriaje/solicitudes/" + solicitudId + "/reservar")
                        .header("Authorization", recepcionistaToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.turnoId").isNumber())
                .andExpect(jsonPath("$.consentimientoInformado").value(true))
                .andExpect(jsonPath("$.grabacionHabilitada").value(false))
                .andReturn();

        Long citaId = objectMapper.readTree(resultReserva.getResponse().getContentAsString()).get("id").asLong();

        // 4. Intento de reservar OTRO turno en el mismo odontólogo y mismo horario debe fallar con HTTP 409
        var sol2 = new SolicitudTeletriaje(paciente2, "Otro dolor", "Leve", NivelPrioridad.VERDE, false, null);
        sol2 = solicitudRepository.save(sol2);

        var resReq2 = new ReservarTeletriajeRequest(odontologo.getId(), fecha, hora, "Consulta coincidente", true, true);
        mockMvc.perform(post("/api/teletriaje/solicitudes/" + sol2.getId() + "/reservar")
                        .header("Authorization", recepcionistaToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resReq2)))
                .andExpect(status().isConflict())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("disponibilidad")));

        // 5. Paciente 1 puede consultar su propia cita
        mockMvc.perform(get("/api/teletriaje/citas/" + citaId)
                        .header("Authorization", paciente1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pacienteId").value(paciente1.getId()));

        // 6. Paciente 2 intenta consultar la cita de Paciente 1 -> 403 Forbidden
        mockMvc.perform(get("/api/teletriaje/citas/" + citaId)
                        .header("Authorization", paciente2Token))
                .andExpect(status().isForbidden())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("solo puede consultar sus propias citas")));
    }
}

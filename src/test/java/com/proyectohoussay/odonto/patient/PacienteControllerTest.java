package com.proyectohoussay.odonto.patient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

@WebMvcTest(PacienteController.class)
@AutoConfigureMockMvc(addFilters = false)
class PacienteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PacienteService pacienteService;

    @Test
    void postRegistersPatientThroughService() throws Exception {
        when(pacienteService.registrarPaciente(any(Paciente.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/pacientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "Ana",
                                  "apellido": "Pérez",
                                  "dni": "12345678",
                                  "telefono": "1123456789",
                                  "email": "ana@example.com",
                                  "fechaNacimiento": "1990-01-01"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().string("Paciente registrado correctamente"));

        verify(pacienteService).registrarPaciente(any(Paciente.class));
    }

    @Test
    void postReturnsConflictWhenServiceRejectsDuplicateDni() throws Exception {
        when(pacienteService.registrarPaciente(any(Paciente.class)))
                .thenThrow(new PacienteDuplicadoException("Ya existe un paciente registrado con ese DNI"));

        mockMvc.perform(post("/api/pacientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "Ana",
                                  "apellido": "Pérez",
                                  "dni": "12345678",
                                  "telefono": "1123456789",
                                  "email": "ana@example.com",
                                  "fechaNacimiento": "1990-01-01"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(content().string("Ya existe un paciente registrado con ese DNI"));
    }

    @Test
    void postReturnsValidationMessageAndDoesNotCallServiceForInvalidPatient() throws Exception {
        mockMvc.perform(post("/api/pacientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "",
                                  "apellido": "Pérez",
                                  "dni": "12345678",
                                  "telefono": "1123456789",
                                  "email": "ana@example.com",
                                  "fechaNacimiento": "1990-01-01"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("El nombre es obligatorio"));

        verifyNoInteractions(pacienteService);
    }

    @Test
    void postReturnsValidationMessageForInvalidEmail() throws Exception {
        mockMvc.perform(post("/api/pacientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "Ana",
                                  "apellido": "Pérez",
                                  "dni": "12345678",
                                  "telefono": "1123456789",
                                  "email": "correo-no-valido",
                                  "fechaNacimiento": "1990-01-01"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("El correo electrónico no tiene un formato válido"));

        verifyNoInteractions(pacienteService);
    }

    @Test
    void postReturnsValidationMessageForFutureBirthDate() throws Exception {
        String tomorrow = LocalDate.now().plusDays(1).toString();
        mockMvc.perform(post("/api/pacientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "Ana",
                                  "apellido": "Pérez",
                                  "dni": "12345678",
                                  "telefono": "1123456789",
                                  "email": "ana@example.com",
                                  "fechaNacimiento": "%s"
                                }
                                """.formatted(tomorrow)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("La fecha de nacimiento no puede ser futura"));

        verifyNoInteractions(pacienteService);
    }

    @Test
    void postAcceptsBirthDateToday() throws Exception {
        when(pacienteService.registrarPaciente(any(Paciente.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/pacientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "Ana",
                                  "apellido": "Pérez",
                                  "dni": "12345678",
                                  "telefono": "1123456789",
                                  "email": "ana@example.com",
                                  "fechaNacimiento": "%s"
                                }
                                """.formatted(LocalDate.now())))
                .andExpect(status().isOk())
                .andExpect(content().string("Paciente registrado correctamente"));

        verify(pacienteService).registrarPaciente(any(Paciente.class));
    }

    @Test
    void postReturnsValidationMessageForMissingBirthDate() throws Exception {
        mockMvc.perform(post("/api/pacientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "Ana",
                                  "apellido": "Pérez",
                                  "dni": "12345678",
                                  "telefono": "1123456789",
                                  "email": "ana@example.com"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("La fecha de nacimiento es obligatoria"));

        verifyNoInteractions(pacienteService);
    }

    @Test
    void getReturnsDtoAndNotFoundWhenMissing() throws Exception {
        Paciente patient = new Paciente();
        patient.setNombre("Ana");
        patient.setApellido("Pérez");
        patient.setDni("12345678");
        patient.setEmail("ana@example.com");
        patient.setFechaNacimiento(LocalDate.of(1990, 1, 1));
        when(pacienteService.listarPacientes()).thenReturn(List.of(patient));
        when(pacienteService.obtenerPaciente(9L)).thenThrow(new PacienteNoEncontradoException(9L));

        mockMvc.perform(get("/api/pacientes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Ana"));
        mockMvc.perform(get("/api/pacientes/9"))
                .andExpect(status().isNotFound());
    }

    @Test
    void putValidatesAndDeleteReturnsNoContent() throws Exception {
        Paciente patient = new Paciente();
        patient.setNombre("Ana");
        patient.setApellido("Pérez");
        patient.setDni("12345678");
        patient.setEmail("ana@example.com");
        patient.setFechaNacimiento(LocalDate.of(1990, 1, 1));
        when(pacienteService.actualizarPaciente(org.mockito.ArgumentMatchers.eq(5L), any(Paciente.class)))
                .thenReturn(patient);

        mockMvc.perform(put("/api/pacientes/5").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Ana","apellido":"Pérez","dni":"12345678",
                                 "email":"ana@example.com","fechaNacimiento":"1990-01-01"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dni").value("12345678"));
        mockMvc.perform(delete("/api/pacientes/5"))
                .andExpect(status().isNoContent());
        verify(pacienteService).eliminarPaciente(5L);
    }
}

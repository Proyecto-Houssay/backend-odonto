package com.proyectohoussay.odonto.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyectohoussay.odonto.chatbot.ChatbotService;
import com.proyectohoussay.odonto.chatbot.dto.ChatRequest;
import com.proyectohoussay.odonto.chatbot.dto.ChatResponse;
import com.proyectohoussay.odonto.repository.MovimientoFinancieroRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.auth.jwt-secret=integration-test-key-with-at-least-thirty-two-bytes",
        "app.auth.admin.username=admin-test",
        "app.auth.admin.password=admin-pass-test",
        "app.auth.patient.username=patient-test",
        "app.auth.patient.password=patient-pass-test"
})
@AutoConfigureMockMvc
class RoleAuthorizationIntegrationTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtTokenService jwtTokenService;
    @Autowired private MovimientoFinancieroRepository movimientoRepository;

    @MockitoBean
    private ChatbotService chatbotService;

    @BeforeEach
    void clearFinancialData() {
        movimientoRepository.deleteAll();
    }

    @Test
    void loginEmiteTokenFirmadoConRolConfigurado() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"usernameOrEmail":"patient-test","password":"patient-pass-test"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value("PATIENT"))
                .andExpect(jsonPath("$.expiresInSeconds").value(3600));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usernameOrEmail\":\"patient-test\",\"password\":\"incorrecta\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void patientPuedeConsultarChatPeroNoInformesFinancierosNiAdministrarConocimiento() throws Exception {
        when(chatbotService.procesarMensaje(any(ChatRequest.class)))
                .thenReturn(ChatResponse.simple("Respuesta documentada", "consulta_general"));
        String patient = authorization("PATIENT");

        mockMvc.perform(post("/api/chatbot/ask")
                        .header("Authorization", patient)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mensaje\":\"¿Cuál es el horario?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.respuesta").value("Respuesta documentada"));

        mockMvc.perform(get("/api/reports/ganancias")
                        .header("Authorization", patient)
                        .param("desde", LocalDate.now().toString())
                        .param("hasta", LocalDate.now().toString()))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/chatbot/knowledge")
                        .header("Authorization", patient)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"source\":\"faq\",\"version\":\"1\",\"content\":\"## FAQ\\nContenido\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/reports/inventario")
                        .header("Authorization", patient)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Insumo\",\"categoria\":\"Prueba\",\"cantidadDisponible\":1,\"unidadMedida\":\"unidad\",\"stockMinimo\":0}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void recepcionistaPuedeConsultarCobrosPeroNoRegistrarMovimientos() throws Exception {
        String receptionist = authorization("RECEPCIONISTA");
        String today = LocalDate.now().toString();

        mockMvc.perform(get("/api/reports/cobros")
                        .header("Authorization", receptionist)
                        .param("desde", today)
                        .param("hasta", today))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/reports/ganancias")
                        .header("Authorization", receptionist)
                        .param("desde", today)
                        .param("hasta", today))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/finanzas/movimientos")
                        .header("Authorization", receptionist)
                        .contentType("application/json")
                        .content("{\"tipo\":\"EGRESO\",\"fecha\":\"" + today + "\",\"importe\":100,\"concepto\":\"No permitido\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void rutasDeInformesRequierenAutenticacionYAdminAccedeAlResumen() throws Exception {
        mockMvc.perform(get("/api/reports"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/reports/ganancias")
                        .header("Authorization", authorization("ADMIN"))
                        .param("desde", LocalDate.now().toString())
                        .param("hasta", LocalDate.now().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ingresosRegistrados").value(0))
                .andExpect(jsonPath("$.egresosRegistrados").value(0))
                .andExpect(jsonPath("$.sinRegistros").value(true));
    }

    private String authorization(String role) {
        return "Bearer " + jwtTokenService.issue("integration-user", role);
    }
}

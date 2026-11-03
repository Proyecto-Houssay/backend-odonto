package com.proyectohoussay.odonto.chatbot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyectohoussay.odonto.chatbot.dto.ChatRequest;
import com.proyectohoussay.odonto.chatbot.dto.ChatResponse;
import com.proyectohoussay.odonto.chatbot.rag.KnowledgeAdminService;
import com.proyectohoussay.odonto.auth.RoleAuthorizationInterceptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doReturn;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChatbotController.class)
class ChatbotControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ChatbotService chatbotService;

    @MockitoBean
    private KnowledgeAdminService knowledgeAdminService;

    @MockitoBean
    private RoleAuthorizationInterceptor roleAuthorizationInterceptor;

    @BeforeEach
    void allowAnnotatedRoutesInControllerSlice() throws Exception {
        doReturn(true).when(roleAuthorizationInterceptor).preHandle(any(), any(), any());
    }

    @Test
    void ask_conMensajeValido_retorna200YRespuesta() throws Exception {
        ChatResponse mockResponse = ChatResponse.conAcciones(
                "Para el dolor de muela de juicio podés tomar ibuprofeno...",
                "urgencia",
                List.of("📞 Llamar a la clínica: (011) 4555-1234", "🏥 Agendar turno de urgencia")
        );
        when(chatbotService.procesarMensaje(any(ChatRequest.class))).thenReturn(mockResponse);

        ChatRequest request = new ChatRequest("me duele la muela de juicio hace 3 días", "session-1");

        mockMvc.perform(post("/api/chatbot/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.respuesta").value(mockResponse.respuesta()))
                .andExpect(jsonPath("$.intencion").value("urgencia"))
                .andExpect(jsonPath("$.accionesSugeridas[0]").exists())
                .andExpect(jsonPath("$.citas").isArray())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void ask_conMensajeVacio_retorna400BadRequest() throws Exception {
        ChatRequest request = new ChatRequest("", "session-1");

        mockMvc.perform(post("/api/chatbot/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}

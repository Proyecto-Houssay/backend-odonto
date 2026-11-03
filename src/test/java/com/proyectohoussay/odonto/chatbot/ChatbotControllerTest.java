package com.proyectohoussay.odonto.chatbot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyectohoussay.odonto.chatbot.dto.ChatRequest;
import com.proyectohoussay.odonto.chatbot.dto.ChatResponse;
import com.proyectohoussay.odonto.chatbot.dto.KnowledgeDocumentRequest;
import com.proyectohoussay.odonto.chatbot.rag.KnowledgeAdminService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChatbotController.class)
@AutoConfigureMockMvc(addFilters = false)
class ChatbotControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ChatbotService chatbotService;

    @MockitoBean
    private KnowledgeAdminService knowledgeAdminService;

    @Test
    void ask_conMensajeValido_retorna200YRespuesta() throws Exception {
        ChatResponse mockResponse = ChatResponse.conCitas(
                "La clínica atiende de lunes a viernes.",
                "consulta_general",
                List.of(),
                List.of(new ChatResponse.CitaDocumento("faq-1", "faq.md — Horarios", "1", "Lunes a viernes"))
        );
        when(chatbotService.procesarMensaje(any(ChatRequest.class))).thenReturn(mockResponse);

        ChatRequest request = new ChatRequest("¿Qué horarios tienen?", "session-1");

        mockMvc.perform(post("/api/chatbot/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.respuesta").value(mockResponse.respuesta()))
                .andExpect(jsonPath("$.intencion").value("consulta_general"))
                .andExpect(jsonPath("$.citas[0].documento").value("faq.md — Horarios"))
                .andExpect(jsonPath("$.citas[0].version").value("1"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void ingestKnowledgeReturnsNoContentAfterValidation() throws Exception {
        KnowledgeDocumentRequest request = new KnowledgeDocumentRequest("faq", "2", "## Horarios\nAtención de lunes a viernes.");

        mockMvc.perform(post("/api/chatbot/knowledge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        org.mockito.Mockito.verify(knowledgeAdminService).ingest(request);
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

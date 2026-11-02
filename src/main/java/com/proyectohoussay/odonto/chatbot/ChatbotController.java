package com.proyectohoussay.odonto.chatbot;

import com.proyectohoussay.odonto.chatbot.dto.ChatRequest;
import com.proyectohoussay.odonto.chatbot.dto.ChatResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST del chatbot inteligente con IA + RAG.
 * Expone un único endpoint POST /api/chatbot/ask que recibe la consulta
 * del paciente y devuelve una respuesta generada por IA fundamentada
 * en la base de conocimiento de la Clínica Novadent.
 *
 * Funcionalidades:
 * - Responde preguntas frecuentes (FAQ)
 * - Informa precios según obra social consultando el nomenclador
 * - Realiza triage de urgencias dentales
 * - Orienta sobre turnos y servicios disponibles
 */
@RestController
@RequestMapping("/api/chatbot")
@CrossOrigin(origins = "*")
public class ChatbotController {

    private final ChatbotService chatbotService;

    public ChatbotController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    /**
     * Endpoint principal del chatbot.
     *
     * Ejemplo de request:
     * POST /api/chatbot/ask
     * {
     *   "mensaje": "me duele la muela de juicio hace 3 días",
     *   "sessionId": "abc123"
     * }
     *
     * Ejemplo de response:
     * {
     *   "respuesta": "Entiendo que tenés dolor de muela de juicio...",
     *   "intencion": "urgencia",
     *   "accionesSugeridas": ["📞 Llamar a la clínica", "🏥 Agendar turno de urgencia"],
     *   "timestamp": "2026-11-02T17:30:00"
     * }
     */
    @PostMapping("/ask")
    public ResponseEntity<ChatResponse> ask(@Valid @RequestBody ChatRequest request) {
        ChatResponse response = chatbotService.procesarMensaje(request);
        return ResponseEntity.ok(response);
    }
}

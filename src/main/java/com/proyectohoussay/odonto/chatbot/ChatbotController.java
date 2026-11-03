package com.proyectohoussay.odonto.chatbot;

import com.proyectohoussay.odonto.chatbot.dto.ChatRequest;
import com.proyectohoussay.odonto.chatbot.dto.ChatResponse;
import com.proyectohoussay.odonto.chatbot.dto.KnowledgeDocumentRequest;
import com.proyectohoussay.odonto.chatbot.rag.KnowledgeAdminService;
import com.proyectohoussay.odonto.auth.RequireRoles;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;

/**
 * Controlador REST del chatbot inteligente con IA + RAG.
 * Expone un único endpoint POST /api/chatbot/ask que recibe la consulta
 * del paciente y devuelve una respuesta generada por IA fundamentada
 * en la base de conocimiento de la Clínica Novadent.
 *
 * Responde consultas generales con citas de documentos aprobados y permite
 * gestionar las fuentes con una credencial administrativa configurada.
 */
@RestController
@RequestMapping("/api/chatbot")
@CrossOrigin(origins = "*")
public class ChatbotController {

    private final ChatbotService chatbotService;
    private final KnowledgeAdminService knowledgeAdminService;

    public ChatbotController(ChatbotService chatbotService, KnowledgeAdminService knowledgeAdminService) {
        this.chatbotService = chatbotService;
        this.knowledgeAdminService = knowledgeAdminService;
    }

    /**
     * Endpoint principal del chatbot.
     *
     * Ejemplo de request:
     * POST /api/chatbot/ask
     * {
     *   "mensaje": "¿Qué información tiene la clínica sobre horarios?",
     *   "sessionId": "abc123"
     * }
     *
     * Ejemplo de response:
     * {
     *   "respuesta": "...",
     *   "intencion": "consulta_general",
     *   "accionesSugeridas": [],
     *   "citas": [{"documento":"faq.md — Horarios","version":"1","fragmento":"..."}],
     *   "timestamp": "2026-11-02T17:30:00"
     * }
     */
    @PostMapping("/ask")
    @RequireRoles({"PATIENT", "ADMIN"})
    public ResponseEntity<ChatResponse> ask(@Valid @RequestBody ChatRequest request) {
        ChatResponse response = chatbotService.procesarMensaje(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/knowledge")
    @RequireRoles("ADMIN")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void ingestKnowledge(@Valid @RequestBody KnowledgeDocumentRequest request) {
        knowledgeAdminService.ingest(request);
    }

    @DeleteMapping("/knowledge/{source}")
    @RequireRoles("ADMIN")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteKnowledge(@PathVariable String source) {
        knowledgeAdminService.delete(source);
    }
}

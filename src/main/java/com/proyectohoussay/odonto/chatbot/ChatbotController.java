package com.proyectohoussay.odonto.chatbot;

import com.proyectohoussay.odonto.chatbot.dto.ChatRequest;
import com.proyectohoussay.odonto.chatbot.dto.ChatResponse;
import com.proyectohoussay.odonto.chatbot.dto.KnowledgeDocumentRequest;
import com.proyectohoussay.odonto.chatbot.rag.KnowledgeAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Endpoints del asistente RAG: consultas con citas, estado del proveedor/configuración
 * y gestión administrativa de documentos aprobados. La autorización de cada ruta se aplica en Spring Security.
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

    /** Responde consultas generales con evidencia de la base aprobada y devuelve citas. */
    @PostMapping("/ask")
    public ResponseEntity<ChatResponse> ask(@Valid @RequestBody ChatRequest request) {
        ChatResponse response = chatbotService.procesarMensaje(request);
        return ResponseEntity.ok(response);
    }

    /** Consulta el estado del proveedor de IA y las fuentes aprobadas cargadas. */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getConfig() {
        return ResponseEntity.ok(chatbotService.obtenerEstadoConfiguracion());
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(chatbotService.obtenerEstadoConfiguracion());
    }

    @PostMapping("/knowledge")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void ingestKnowledge(@Valid @RequestBody KnowledgeDocumentRequest request) {
        knowledgeAdminService.ingest(request);
    }

    @DeleteMapping("/knowledge/{source}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteKnowledge(@PathVariable String source) {
        knowledgeAdminService.delete(source);
    }
}

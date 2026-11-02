package com.proyectohoussay.odonto.chatbot;

import com.proyectohoussay.odonto.chatbot.dto.ChatRequest;
import com.proyectohoussay.odonto.chatbot.dto.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Servicio principal del chatbot odontológico.
 * Utiliza Spring AI con RAG (Retrieval-Augmented Generation) para responder
 * consultas de pacientes basándose en la base de conocimiento de la clínica.
 *
 * El flujo es:
 * 1. El usuario envía un mensaje.
 * 2. Se busca en el VectorStore las secciones más relevantes de la knowledge base.
 * 3. Se inyectan esas secciones como contexto en el prompt del LLM.
 * 4. El LLM genera una respuesta fundamentada en datos reales de la clínica.
 * 5. Se clasifica la intención y se sugieren acciones al usuario.
 */
@Service
public class ChatbotService {

    private static final Logger log = LoggerFactory.getLogger(ChatbotService.class);
    private static final int TOP_K_RESULTS = 5;

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    public ChatbotService(ChatClient chatClient, VectorStore vectorStore) {
        this.chatClient = chatClient;
        this.vectorStore = vectorStore;
    }

    /**
     * Procesa un mensaje del usuario usando RAG.
     * Busca contexto relevante en la knowledge base y genera una respuesta.
     */
    public ChatResponse procesarMensaje(ChatRequest request) {
        log.info("Procesando mensaje del chatbot: '{}'", truncate(request.mensaje(), 80));

        try {
            // Construir el advisor RAG que inyecta contexto relevante
            SearchRequest searchRequest = SearchRequest.builder()
                    .query(request.mensaje())
                    .topK(TOP_K_RESULTS)
                    .build();

            QuestionAnswerAdvisor ragAdvisor = new QuestionAnswerAdvisor(vectorStore, searchRequest);

            // Generar respuesta con RAG
            String respuesta = chatClient.prompt()
                    .advisors(ragAdvisor)
                    .user(request.mensaje())
                    .call()
                    .content();

            // Clasificar intención y generar acciones sugeridas
            String intencion = clasificarIntencion(request.mensaje());
            List<String> acciones = generarAccionesSugeridas(intencion);

            log.info("Respuesta generada exitosamente. Intención: {}", intencion);
            return ChatResponse.conAcciones(respuesta, intencion, acciones);

        } catch (Exception e) {
            log.error("Error al procesar mensaje del chatbot: {}", e.getMessage(), e);
            return ChatResponse.simple(
                "Disculpá, hubo un error al procesar tu consulta. "
                + "Por favor, intentá de nuevo o comunicate directamente con la clínica "
                + "al (011) 4555-1234.",
                "error"
            );
        }
    }

    /**
     * Clasifica la intención del mensaje del usuario basándose en palabras clave.
     * En producción, esto podría ser otro modelo de IA o NLP más sofisticado.
     */
    String clasificarIntencion(String mensaje) {
        String lower = mensaje.toLowerCase();

        // Urgencias
        if (containsAny(lower, "dolor", "duele", "hinchado", "hinchazón", "sangra",
                "sangrado", "urgencia", "emergencia", "golpe", "caído", "quebr",
                "roto", "absceso", "fiebre", "no puedo abrir la boca")) {
            return "urgencia";
        }

        // Precios y costos
        if (containsAny(lower, "precio", "costo", "cuánto sale", "cuanto sale",
                "cuánto cuesta", "cuanto cuesta", "valor", "tarifa", "copago",
                "obra social", "osde", "swiss", "galeno", "medicus", "ioma", "pami")) {
            return "precio";
        }

        // Turnos
        if (containsAny(lower, "turno", "cita", "agendar", "reservar", "disponib",
                "horario", "cancelar turno", "cambiar turno")) {
            return "turno";
        }

        // Tratamientos y servicios
        if (containsAny(lower, "tratamiento", "blanqueamiento", "ortodoncia",
                "implante", "conducto", "corona", "prótesis", "limpieza",
                "brackets", "carilla", "extracción", "muela de juicio",
                "endodoncia", "periodoncia")) {
            return "tratamiento";
        }

        // Información general
        return "consulta_general";
    }

    /**
     * Genera acciones sugeridas para el frontend según la intención detectada.
     */
    private List<String> generarAccionesSugeridas(String intencion) {
        List<String> acciones = new ArrayList<>();

        switch (intencion) {
            case "urgencia" -> {
                acciones.add("📞 Llamar a la clínica: (011) 4555-1234");
                acciones.add("🏥 Agendar turno de urgencia");
                acciones.add("📋 Ver protocolos de emergencia");
            }
            case "precio" -> {
                acciones.add("💰 Ver nomenclador completo de precios");
                acciones.add("🏥 Consultar cobertura de mi obra social");
                acciones.add("📅 Agendar consulta presupuesto");
            }
            case "turno" -> {
                acciones.add("📅 Agendar nuevo turno");
                acciones.add("❌ Cancelar turno existente");
                acciones.add("🔍 Consultar mis turnos");
            }
            case "tratamiento" -> {
                acciones.add("📅 Agendar consulta de evaluación");
                acciones.add("💰 Consultar precio del tratamiento");
                acciones.add("ℹ️ Más información sobre el tratamiento");
            }
            default -> {
                acciones.add("📅 Agendar turno");
                acciones.add("💰 Consultar precios");
                acciones.add("📞 Contactar a la clínica");
            }
        }

        return acciones;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "...";
    }
}

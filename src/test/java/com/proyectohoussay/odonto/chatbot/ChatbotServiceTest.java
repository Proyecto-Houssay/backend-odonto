package com.proyectohoussay.odonto.chatbot;

import com.proyectohoussay.odonto.chatbot.dto.ChatRequest;
import com.proyectohoussay.odonto.chatbot.dto.ChatResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChatbotServiceTest {

    private final ChatClient chatClient = mock(ChatClient.class, Mockito.RETURNS_DEEP_STUBS);
    private final VectorStore vectorStore = mock(VectorStore.class);
    private final ChatbotService chatbotService = new ChatbotService(chatClient, vectorStore);

    @ParameterizedTest
    @ValueSource(strings = {
        "Me duele mucho la muela de juicio hace 3 días",
        "Tengo la cara hinchada y fiebre",
        "Se me cayó un diente por un golpe urgente",
        "Tengo un dolor insoportable"
    })
    void clasificarIntencion_deberiaDetectarUrgencias(String mensaje) {
        String intencion = chatbotService.clasificarIntencion(mensaje);
        assertEquals("urgencia", intencion);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "¿Cuánto sale una limpieza con OSDE?",
        "Precio de una extracción particular",
        "Cuánto cuesta un implante con Swiss Medical",
        "Tarifas y copago para conducto"
    })
    void clasificarIntencion_deberiaDetectarPrecios(String mensaje) {
        String intencion = chatbotService.clasificarIntencion(mensaje);
        assertEquals("precio", intencion);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "Quiero sacar un turno para la semana que viene",
        "¿Hay disponibilidad de citas para mañana?",
        "Necesito cancelar mi turno de las 15 hs"
    })
    void clasificarIntencion_deberiaDetectarTurnos(String mensaje) {
        String intencion = chatbotService.clasificarIntencion(mensaje);
        assertEquals("turno", intencion);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "Quería consultar por ortodoncia y brackets",
        "Hacen blanqueamiento dental?",
        "Información sobre prótesis o implantes"
    })
    void clasificarIntencion_deberiaDetectarTratamientos(String mensaje) {
        String intencion = chatbotService.clasificarIntencion(mensaje);
        assertEquals("tratamiento", intencion);
    }

    @Test
    void clasificarIntencion_consultaGeneral() {
        String intencion = chatbotService.clasificarIntencion("Hola, buenas tardes, qué días atienden?");
        assertEquals("consulta_general", intencion);
    }

    @Test
    void procesarMensaje_manejoDeErrores_devuelveRespuestaAmigable() {
        // Simulamos un fallo arrojado por el vectorStore o búsqueda
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenThrow(new RuntimeException("Simulated VectorStore failure"));

        ChatRequest request = new ChatRequest("Hola", "session-123");
        ChatResponse response = chatbotService.procesarMensaje(request);

        assertNotNull(response);
        assertEquals("error", response.intencion());
        assertTrue(response.respuesta().contains("Disculpá, hubo un error"));
    }
}

package com.proyectohoussay.odonto.chatbot.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO de respuesta del chatbot.
 * Incluye la respuesta generada por IA, tipo de intención detectada,
 * acciones sugeridas, citas de fuentes y opcionalmente agenda interactiva real.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatResponse(
    String respuesta,
    String intencion,
    List<String> accionesSugeridas,
    LocalDateTime timestamp,
    List<CitaDocumento> citas,
    AgendaAsistenteDto agenda
) {
    public record CitaDocumento(String id, String documento, String version, String fragmento) {}

    /**
     * Crea una respuesta simple sin acciones sugeridas.
     */
    public static ChatResponse simple(String respuesta, String intencion) {
        return new ChatResponse(respuesta, intencion, List.of(), LocalDateTime.now(), List.of(), null);
    }

    /**
     * Crea una respuesta con acciones sugeridas para el usuario.
     */
    public static ChatResponse conAcciones(String respuesta, String intencion, List<String> acciones) {
        return new ChatResponse(respuesta, intencion, acciones, LocalDateTime.now(), List.of(), null);
    }

    public static ChatResponse conCitas(String respuesta, String intencion, List<String> acciones,
                                         List<CitaDocumento> citas) {
        return new ChatResponse(respuesta, intencion, acciones, LocalDateTime.now(), List.copyOf(citas), null);
    }

    public static ChatResponse conAgenda(String respuesta, String intencion, List<String> acciones,
                                          AgendaAsistenteDto agenda) {
        return new ChatResponse(respuesta, intencion, acciones, LocalDateTime.now(), List.of(), agenda);
    }
}

package com.proyectohoussay.odonto.chatbot;

import com.proyectohoussay.odonto.chatbot.rag.KnowledgeBaseLoader;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class KnowledgeBaseLoaderTest {

    private final KnowledgeBaseLoader loader = new KnowledgeBaseLoader(mock(VectorStore.class));

    @Test
    void splitIntoChunks_agregaFuenteVersionYFragmentoEstable() {
        String content = "# Política\nDocumento aprobado de la clínica.\n\n## Turnos\nLos turnos se solicitan por recepción.";

        List<Document> first = loader.splitIntoChunks(content, "politica.md", "2026-10");
        List<Document> second = loader.splitIntoChunks(content, "politica.md", "2026-10");
        List<Document> nextVersion = loader.splitIntoChunks(content, "politica.md", "2026-11");

        assertEquals(2, first.size());
        assertEquals("politica.md", first.get(1).getMetadata().get("source"));
        assertEquals("2026-10", first.get(1).getMetadata().get("version"));
        assertEquals(first.get(1).getId(), second.get(1).getId());
        assertNotEquals(first.get(1).getId(), nextVersion.get(1).getId());
    }
}

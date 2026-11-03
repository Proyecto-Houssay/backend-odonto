package com.proyectohoussay.odonto.chatbot;

import com.proyectohoussay.odonto.chatbot.dto.KnowledgeDocumentRequest;
import com.proyectohoussay.odonto.chatbot.rag.KnowledgeAdminService;
import com.proyectohoussay.odonto.chatbot.rag.KnowledgeBaseLoader;
import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ai.document.Document;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class KnowledgeAdminServiceTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void reemplazoYRetiroEliminanLosFragmentosAnterioresDelIndice() throws Exception {
        VectorStore vectorStore = mock(VectorStore.class);
        KnowledgeBaseLoader loader = new KnowledgeBaseLoader(vectorStore);
        KnowledgeAdminService service = new KnowledgeAdminService(vectorStore, loader);
        ReflectionTestUtils.setField(service, "apiKey", "configured-key");
        ReflectionTestUtils.setField(service, "managedDirectory", temporaryDirectory.toString());
        ReflectionTestUtils.setField(service, "defaultVersion", "1");

        service.ingest(new KnowledgeDocumentRequest("faq", "1", "## Horarios\nAtención de lunes a viernes."));
        ArgumentCaptor<List<Document>> initial = ArgumentCaptor.forClass(List.class);
        verify(vectorStore).add(initial.capture());
        List<String> oldIds = initial.getValue().stream().map(Document::getId).toList();

        clearInvocations(vectorStore);
        service.ingest(new KnowledgeDocumentRequest("faq", "2", "## Horarios\nAtención de lunes a sábado."));
        verify(vectorStore).delete(oldIds);
        ArgumentCaptor<List<Document>> replacement = ArgumentCaptor.forClass(List.class);
        verify(vectorStore).add(replacement.capture());
        List<String> newIds = replacement.getValue().stream().map(Document::getId).toList();
        assertNotEquals(oldIds, newIds);
        assertEquals("2", Files.readString(temporaryDirectory.resolve("faq.md.version")));

        clearInvocations(vectorStore);
        service.delete("faq");
        verify(vectorStore).delete(newIds);
        assertFalse(Files.exists(temporaryDirectory.resolve("faq.md")));
        assertFalse(Files.exists(temporaryDirectory.resolve("faq.md.version")));
    }
}

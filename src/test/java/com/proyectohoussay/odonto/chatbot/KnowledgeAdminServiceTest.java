package com.proyectohoussay.odonto.chatbot;

import com.proyectohoussay.odonto.chatbot.dto.KnowledgeDocumentRequest;
import com.proyectohoussay.odonto.chatbot.rag.KnowledgeAdminService;
import com.proyectohoussay.odonto.chatbot.rag.KnowledgeBaseLoader;
import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
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
    void ingest_sinCredencialAdministrativaRechazaLaOperacion() {
        KnowledgeAdminService service = new KnowledgeAdminService(
                mock(VectorStore.class), new KnowledgeBaseLoader(mock(VectorStore.class)));
        ReflectionTestUtils.setField(service, "adminToken", "secreto");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.ingest(new KnowledgeDocumentRequest("faq", "1", "## FAQ\nContenido aprobado."), null));

        assertEquals(403, exception.getStatusCode().value());
    }

    @Test
    void reemplazoYRetiroEliminanLosFragmentosAnterioresDelIndice() throws Exception {
        VectorStore vectorStore = mock(VectorStore.class);
        KnowledgeBaseLoader loader = new KnowledgeBaseLoader(vectorStore);
        KnowledgeAdminService service = new KnowledgeAdminService(vectorStore, loader);
        ReflectionTestUtils.setField(service, "adminToken", "admin-secreto");
        ReflectionTestUtils.setField(service, "apiKey", "configured-key");
        ReflectionTestUtils.setField(service, "managedDirectory", temporaryDirectory.toString());
        ReflectionTestUtils.setField(service, "defaultVersion", "1");

        service.ingest(new KnowledgeDocumentRequest("faq", "1", "## Horarios\nAtención de lunes a viernes."), "admin-secreto");
        ArgumentCaptor<List<Document>> initial = ArgumentCaptor.forClass(List.class);
        verify(vectorStore).add(initial.capture());
        List<String> oldIds = initial.getValue().stream().map(Document::getId).toList();

        clearInvocations(vectorStore);
        service.ingest(new KnowledgeDocumentRequest("faq", "2", "## Horarios\nAtención de lunes a sábado."), "admin-secreto");
        verify(vectorStore).delete(oldIds);
        ArgumentCaptor<List<Document>> replacement = ArgumentCaptor.forClass(List.class);
        verify(vectorStore).add(replacement.capture());
        List<String> newIds = replacement.getValue().stream().map(Document::getId).toList();
        assertNotEquals(oldIds, newIds);
        assertEquals("2", Files.readString(temporaryDirectory.resolve("faq.md.version")));

        clearInvocations(vectorStore);
        service.delete("faq", "admin-secreto");
        verify(vectorStore).delete(newIds);
        assertFalse(Files.exists(temporaryDirectory.resolve("faq.md")));
        assertFalse(Files.exists(temporaryDirectory.resolve("faq.md.version")));
    }
}

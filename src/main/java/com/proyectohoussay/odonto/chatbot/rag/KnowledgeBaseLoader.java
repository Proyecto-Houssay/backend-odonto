package com.proyectohoussay.odonto.chatbot.rag;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Carga la base de conocimiento de la cl??nica dental en el VectorStore
 * al iniciar la aplicaci??n. Los documentos se dividen en secciones (chunks)
 * para mejorar la precisi??n de la b??squeda sem??ntica (RAG).
 */
@Component
public class KnowledgeBaseLoader {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseLoader.class);
    private static final String KNOWLEDGE_PATH = "classpath:knowledge/*.md";

    private final VectorStore vectorStore;

    @Value("${spring.ai.openai.api-key:mock-key-for-local-dev}")
    private String apiKey;

    public KnowledgeBaseLoader(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    /**
     * Carga todos los archivos .md de la carpeta knowledge/ al iniciar la app.
     * Si no hay API key real configurada (por ejemplo en tests locales o CI),
     * captura cualquier excepci??n de red/autenticaci??n para permitir que la app
     * y los tests de integraci??n levanten normalmente.
     */
    @PostConstruct
    public void loadKnowledgeBase() {
        if (apiKey == null || apiKey.isBlank() || apiKey.startsWith("mock-key")) {
            log.warn("OPENAI_API_KEY no configurada o mock. Se omite la indexaci??n inicial de embeddings.");
            return;
        }

        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources(KNOWLEDGE_PATH);

            List<Document> allDocuments = new ArrayList<>();

            for (Resource resource : resources) {
                String filename = resource.getFilename();
                String content = resource.getContentAsString(StandardCharsets.UTF_8);
                List<Document> chunks = splitIntoChunks(content, filename);
                allDocuments.addAll(chunks);
                log.info("Cargado archivo de conocimiento: {} ({} secciones)", filename, chunks.size());
            }

            if (!allDocuments.isEmpty()) {
                vectorStore.add(allDocuments);
                log.info("Base de conocimiento cargada exitosamente: {} documentos en total", allDocuments.size());
            } else {
                log.warn("No se encontraron archivos de conocimiento en {}", KNOWLEDGE_PATH);
            }

        } catch (Exception e) {
            log.warn("No se pudo conectar con el proveedor de IA para embeddings durante el inicio: {}. La base de conocimiento podr?? indexarse cuando haya conectividad.", e.getMessage());
        }
    }

    /**
     * Divide un archivo markdown en chunks basados en las secciones (##).
     * Cada chunk mantiene el t??tulo de la secci??n como contexto.
     */
    private List<Document> splitIntoChunks(String content, String filename) {
        List<Document> documents = new ArrayList<>();
        String[] sections = content.split("(?=\n## )");

        for (String section : sections) {
            String trimmed = section.trim();
            if (trimmed.isEmpty() || trimmed.length() < 20) {
                continue;
            }

            String title = extractTitle(trimmed);

            Document doc = new Document(trimmed, Map.of(
                "source", filename != null ? filename : "unknown",
                "section", title,
                "type", categorizeDocument(filename)
            ));
            documents.add(doc);
        }

        return documents;
    }

    private String extractTitle(String section) {
        String[] lines = section.split("\n", 2);
        return lines[0].replaceAll("^#+\\s*", "").trim();
    }

    private String categorizeDocument(String filename) {
        if (filename == null) return "general";
        if (filename.contains("faq")) return "faq";
        if (filename.contains("precio")) return "precios";
        if (filename.contains("protocolo")) return "protocolo";
        if (filename.contains("especialidad")) return "servicios";
        return "general";
    }
}

package com.proyectohoussay.odonto.chatbot.rag;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Carga la base de conocimiento de la clínica dental en el VectorStore
 * al iniciar la aplicación. Los documentos se dividen en secciones (chunks)
 * para mejorar la precisión de la búsqueda semántica (RAG).
 */
@Component
public class KnowledgeBaseLoader {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseLoader.class);
    private static final String KNOWLEDGE_PATH = "classpath:knowledge/*.md";

    private final VectorStore vectorStore;

    public KnowledgeBaseLoader(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    /**
     * Carga todos los archivos .md de la carpeta knowledge/ al iniciar la app.
     * Cada sección (delimitada por ## o ###) se convierte en un Document separado
     * para optimizar la búsqueda semántica.
     */
    @PostConstruct
    public void loadKnowledgeBase() {
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

        } catch (IOException e) {
            log.error("Error al cargar la base de conocimiento: {}", e.getMessage(), e);
        }
    }

    /**
     * Divide un archivo markdown en chunks basados en las secciones (##).
     * Cada chunk mantiene el título de la sección como contexto.
     */
    private List<Document> splitIntoChunks(String content, String filename) {
        List<Document> documents = new ArrayList<>();
        String[] sections = content.split("(?=\n## )");

        for (String section : sections) {
            String trimmed = section.trim();
            if (trimmed.isEmpty() || trimmed.length() < 20) {
                continue;
            }

            // Extraer el título de la sección para metadata
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

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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

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

    @Value("${spring.ai.openai.api-key:mock-key-for-local-dev}")
    private String apiKey;

    @Value("${app.chatbot.knowledge-directory:./data/knowledge}")
    private String managedDirectory;

    @Value("${app.chatbot.knowledge-version:1}")
    private String defaultVersion;

    @Value("${app.chatbot.approved-bundled-sources:}")
    private String approvedBundledSources;

    public KnowledgeBaseLoader(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    /**
     * Carga todos los archivos .md de la carpeta knowledge/ al iniciar la app.
     * Si no hay API key real configurada (por ejemplo en tests locales o CI),
     * captura cualquier excepción de red/autenticación para permitir que la app
     * y los tests de integración levanten normalmente.
     */
    @PostConstruct
    public void loadKnowledgeBase() {
        if (apiKey == null || apiKey.isBlank() || apiKey.startsWith("mock-key")) {
            log.warn("OPENAI_API_KEY no configurada o mock. Se omite la indexación inicial de embeddings.");
            return;
        }

        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources(KNOWLEDGE_PATH);

            List<Document> allDocuments = new ArrayList<>();

            for (Resource resource : resources) {
                String filename = resource.getFilename();
                if (!isApprovedBundledSource(filename, approvedBundledSources)) {
                    log.warn("Se omite la fuente de conocimiento empaquetada sin aprobación explícita: {}", filename);
                    continue;
                }
                String content = resource.getContentAsString(StandardCharsets.UTF_8);
                List<Document> chunks = splitIntoChunks(content, filename, defaultVersion);
                allDocuments.addAll(chunks);
                log.info("Cargado archivo de conocimiento: {} ({} secciones)", filename, chunks.size());
            }

            if (!allDocuments.isEmpty()) {
                vectorStore.add(allDocuments);
                log.info("Base de conocimiento cargada exitosamente: {} documentos en total", allDocuments.size());
            } else {
                log.warn("No hay fuentes empaquetadas aprobadas para indexar en {}", KNOWLEDGE_PATH);
            }

            Path managedPath = Paths.get(managedDirectory);
            if (Files.isDirectory(managedPath)) {
                try (var paths = Files.list(managedPath)) {
                    paths.filter(path -> path.getFileName().toString().endsWith(".md")).forEach(path -> {
                        try {
                            String content = Files.readString(path, StandardCharsets.UTF_8);
                            Path versionPath = path.resolveSibling(path.getFileName() + ".version");
                            String version = Files.exists(versionPath)
                                    ? Files.readString(versionPath, StandardCharsets.UTF_8).trim() : defaultVersion;
                            List<Document> managedDocuments = splitIntoChunks(content,
                                    path.getFileName().toString(), version);
                            if (!managedDocuments.isEmpty()) vectorStore.add(managedDocuments);
                        } catch (Exception e) {
                            log.warn("No se pudo indexar el documento administrado {}", path.getFileName());
                        }
                    });
                }
            }

        } catch (Exception e) {
            log.warn("No se pudo conectar con el proveedor de IA para embeddings durante el inicio: {}. La base de conocimiento podrá indexarse cuando haya conectividad.", e.getMessage());
        }
    }

    static boolean isApprovedBundledSource(String filename, String approvedSources) {
        if (filename == null || approvedSources == null || approvedSources.isBlank()) {
            return false;
        }
        return Arrays.stream(approvedSources.split(","))
                .map(String::trim)
                .filter(source -> !source.isBlank())
                .anyMatch(filename::equals);
    }

    /**
     * Divide un archivo markdown en chunks basados en las secciones (##).
     * Cada chunk mantiene el título de la sección como contexto.
     */
    public List<Document> splitIntoChunks(String content, String filename, String version) {
        List<Document> documents = new ArrayList<>();
        String[] sections = content.split("(?=\n## )");

        for (int i = 0; i < sections.length; i++) {
            String section = sections[i];
            String trimmed = section.trim();
            if (trimmed.isEmpty() || trimmed.length() < 20) {
                continue;
            }

            String title = extractTitle(trimmed);

            String source = filename != null ? filename : "unknown";
            String documentId = UUID.nameUUIDFromBytes((source + "|" + version + "|" + i + "|" + trimmed)
                    .getBytes(StandardCharsets.UTF_8)).toString();
            Document doc = new Document(documentId, trimmed, Map.of(
                "source", source,
                "section", title,
                "type", categorizeDocument(filename),
                "version", version,
                "fragment", i
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

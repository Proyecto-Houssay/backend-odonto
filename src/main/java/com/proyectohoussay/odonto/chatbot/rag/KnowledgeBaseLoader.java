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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Carga la base de conocimiento de la clínica dental en el VectorStore
 * y en memoria al iniciar la aplicación. Los documentos se dividen en secciones (chunks)
 * para garantizar respuestas precisas basadas en fuentes aprobadas (RAG),
 * tanto con OpenAI como en modo autónomo local.
 */
@Component
public class KnowledgeBaseLoader {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseLoader.class);
    private static final String KNOWLEDGE_PATH = "classpath:knowledge/*.md";
    public static final String DEFAULT_APPROVED_SOURCES = "especialidades-servicios.md,faq-clinica.md,precios-nomenclador.md,protocolos-urgencia.md";

    private final VectorStore vectorStore;
    private final List<Document> approvedDocuments = new CopyOnWriteArrayList<>();

    @Value("${spring.ai.openai.api-key:mock-key-for-local-dev}")
    private String apiKey;

    @Value("${app.chatbot.knowledge-directory:./data/knowledge}")
    private String managedDirectory;

    @Value("${app.chatbot.knowledge-version:1}")
    private String defaultVersion;

    @Value("${app.chatbot.approved-bundled-sources:" + DEFAULT_APPROVED_SOURCES + "}")
    private String approvedBundledSources;

    public KnowledgeBaseLoader(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    /**
     * Carga todos los archivos .md aprobados de la carpeta knowledge/ al iniciar la app.
     * Mantiene las fuentes aprobadas en memoria para permitir respuestas citadas y triaje
     * en cualquier entorno, e indexa en VectorStore si hay una clave de OpenAI configurada.
     */
    @PostConstruct
    public void loadKnowledgeBase() {
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
                log.info("Cargado archivo de conocimiento aprobado: {} ({} secciones)", filename, chunks.size());
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
                            allDocuments.addAll(managedDocuments);
                        } catch (Exception e) {
                            log.warn("No se pudo indexar el documento administrado {}", path.getFileName());
                        }
                    });
                }
            }

            approvedDocuments.clear();
            approvedDocuments.addAll(allDocuments);
            log.info("Total de secciones aprobadas disponibles en memoria: {}", approvedDocuments.size());

            if (isApiKeyConfigured() && vectorStore != null) {
                try {
                    vectorStore.add(allDocuments);
                    log.info("Base de conocimiento indexada en VectorStore con embeddings: {} documentos", allDocuments.size());
                } catch (Exception e) {
                    log.warn("No se pudo conectar con el proveedor de IA para embeddings durante el inicio: {}. El asistente operará con fuentes aprobadas en memoria.", e.getMessage());
                }
            } else {
                log.info("OPENAI_API_KEY no configurada o mock. El asistente operará con el motor de fuentes aprobadas en memoria ({} secciones).", approvedDocuments.size());
            }

        } catch (Exception e) {
            log.warn("Error cargando archivos de conocimiento aprobados: {}", e.getMessage());
        }
    }

    public static boolean isApprovedBundledSource(String filename, String approvedSources) {
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

    public List<Document> getApprovedDocuments() {
        return Collections.unmodifiableList(approvedDocuments);
    }

    public boolean isApiKeyConfigured() {
        return apiKey != null && !apiKey.isBlank() && !apiKey.startsWith("mock-key");
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

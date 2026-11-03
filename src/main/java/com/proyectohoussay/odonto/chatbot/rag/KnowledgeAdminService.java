package com.proyectohoussay.odonto.chatbot.rag;

import com.proyectohoussay.odonto.chatbot.dto.KnowledgeDocumentRequest;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

@Service
public class KnowledgeAdminService {

    private final VectorStore vectorStore;
    private final KnowledgeBaseLoader loader;

    @Value("${app.chatbot.knowledge-directory:./data/knowledge}")
    private String managedDirectory;

    @Value("${spring.ai.openai.api-key:mock-key-for-local-dev}")
    private String apiKey;

    @Value("${app.chatbot.knowledge-version:1}")
    private String defaultVersion;

    public KnowledgeAdminService(VectorStore vectorStore, KnowledgeBaseLoader loader) {
        this.vectorStore = vectorStore;
        this.loader = loader;
    }

    public synchronized void ingest(KnowledgeDocumentRequest request) {
        ensureEmbeddingsConfigured();

        String filename = request.source() + ".md";
        Path directory = Paths.get(managedDirectory).toAbsolutePath().normalize();
        Path target = directory.resolve(filename).normalize();
        if (!target.getParent().equals(directory)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nombre de fuente inválido");
        }

        List<Document> newDocuments = loader.splitIntoChunks(request.content(), filename, request.version());
        if (newDocuments.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El documento no contiene secciones indexables");
        }

        try {
            Files.createDirectories(directory);
            Path oldVersionPath = target.resolveSibling(filename + ".version");
            String oldContent = Files.exists(target) ? Files.readString(target, StandardCharsets.UTF_8) : null;
            String oldVersion = Files.exists(oldVersionPath)
                    ? Files.readString(oldVersionPath, StandardCharsets.UTF_8).trim() : defaultVersion;
            if (request.content().equals(oldContent) && request.version().equals(oldVersion)) return;
            Path contentTemp = Files.createTempFile(directory, "knowledge-", ".tmp");
            Path versionTemp = Files.createTempFile(directory, "version-", ".tmp");
            try {
                Files.writeString(contentTemp, request.content(), StandardCharsets.UTF_8);
                Files.writeString(versionTemp, request.version(), StandardCharsets.UTF_8);
                vectorStore.add(newDocuments);
                Files.move(contentTemp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                Files.move(versionTemp, oldVersionPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                if (oldContent != null) {
                    List<Document> oldDocuments = loader.splitIntoChunks(oldContent, filename, oldVersion);
                    vectorStore.delete(oldDocuments.stream().map(Document::getId).toList());
                }
            } catch (Exception exception) {
                vectorStore.delete(newDocuments.stream().map(Document::getId).toList());
                throw exception;
            } finally {
                Files.deleteIfExists(contentTemp);
                Files.deleteIfExists(versionTemp);
            }
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "No se pudo guardar o indexar el documento", exception);
        }
    }

    public synchronized void delete(String source) {
        ensureEmbeddingsConfigured();
        if (source == null || !source.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,99}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nombre de fuente inválido");
        }
        String filename = source + ".md";
        Path directory = Paths.get(managedDirectory).toAbsolutePath().normalize();
        Path target = directory.resolve(filename).normalize();
        Path versionPath = target.resolveSibling(filename + ".version");
        if (!target.getParent().equals(directory) || !Files.exists(target)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Documento administrado no encontrado");
        }
        try {
            String content = Files.readString(target, StandardCharsets.UTF_8);
            String version = Files.exists(versionPath)
                    ? Files.readString(versionPath, StandardCharsets.UTF_8).trim() : defaultVersion;
            List<Document> oldDocuments = loader.splitIntoChunks(content, filename, version);
            vectorStore.delete(oldDocuments.stream().map(Document::getId).toList());
            Files.deleteIfExists(target);
            Files.deleteIfExists(versionPath);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "No se pudo retirar el documento", exception);
        }
    }

    private void ensureEmbeddingsConfigured() {
        if (apiKey == null || apiKey.isBlank() || apiKey.startsWith("mock-key")) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "La indexación requiere una clave de embeddings configurada");
        }
    }
}

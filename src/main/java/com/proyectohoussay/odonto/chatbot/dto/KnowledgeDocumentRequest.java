package com.proyectohoussay.odonto.chatbot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record KnowledgeDocumentRequest(
        @NotBlank @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._-]{0,99}") String source,
        @NotBlank @Size(max = 40) @Pattern(regexp = "[A-Za-z0-9._-]{1,40}") String version,
        @NotBlank @Size(max = 100_000) String content) {
}

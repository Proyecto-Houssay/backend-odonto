package com.proyectohoussay.odonto.chatbot.rag;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnowledgeSourcesSafetyTest {

    @Test
    void bundledKnowledgeRequiresExplicitApprovalBeforeIndexing() {
        assertFalse(KnowledgeBaseLoader.isApprovedBundledSource("faq-clinica.md", ""));
        assertFalse(KnowledgeBaseLoader.isApprovedBundledSource("protocolos-urgencia.md", null));
        assertFalse(KnowledgeBaseLoader.isApprovedBundledSource("precios-nomenclador.md", "   "));
        assertTrue(KnowledgeBaseLoader.isApprovedBundledSource("faq-clinica.md", "faq-clinica.md"));
        assertTrue(KnowledgeBaseLoader.isApprovedBundledSource("precios-nomenclador.md",
                "especialidades-servicios.md, precios-nomenclador.md"));
        assertFalse(KnowledgeBaseLoader.isApprovedBundledSource("faq-clinica.md",
                "faq-clinica.md.backup"));
    }
}

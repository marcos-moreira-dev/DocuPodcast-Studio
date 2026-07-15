package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guardrail for T128-C01: root documentation must preserve the product-clean contract. */
final class ProductDocumentationCleanT128C01SourceTest {
    @Test
    void rootDocsDeclareCurrentProductRules() throws Exception {
        String rootDocs = read("README.md") + read("AI_HANDOFF.md") + read("VALIDATION.md");
        assertTrue(rootDocs.contains("T128-C01 — Documentación de limpieza de producto"));
        assertTrue(rootDocs.contains("Markdown se abre como documento fuente"));
        assertTrue(rootDocs.contains("Whisper/STT/audio a texto fue retirado del build principal"));
        assertTrue(rootDocs.contains("PreparedReadingProjection"));
        assertTrue(rootDocs.contains("No existe subir guion"));
    }

    @Test
    void rootDocsDoNotKeepWhisperAsFutureInfrastructure() throws Exception {
        String rootDocs = read("README.md") + read("AI_HANDOFF.md") + read("VALIDATION.md");
        assertFalse(rootDocs.contains("queda solo como infraestructura histórica/futura"));
        assertFalse(rootDocs.contains("Whisper como infraestructura histórica/futura"));
        assertFalse(rootDocs.contains("Completar experiencia Whisper"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}

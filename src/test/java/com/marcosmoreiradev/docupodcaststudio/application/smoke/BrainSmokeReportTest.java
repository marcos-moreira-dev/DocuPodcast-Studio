package com.marcosmoreiradev.docupodcaststudio.application.smoke;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BrainSmokeReportTest {
    @Test
    void summarizesStepsAndRendersMarkdownEvidence() {
        BrainSmokeReport report = new BrainSmokeReport(
                "docupodcast-brain-smoke-v1",
                "Smoke cerebro",
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T00:00:02Z"),
                List.of(
                        BrainSmokeStep.passed("DOC", "Importar", "DOCX/TXT/MD/PDF OK", Duration.ofMillis(25)),
                        BrainSmokeStep.warning("PDF", "Fallback PDF", "PDF escaneado abierto visualmente por OCR no disponible", Duration.ofMillis(5))
                ),
                List.of("target/docupodcast-smoke/SMOKE_REPORT.md")
        );

        assertTrue(report.successful());
        assertEquals(1L, report.warningCount());
        assertEquals("OK_CON_ADVERTENCIAS", report.statusLabel());
        assertTrue(report.toMarkdown().contains("docupodcast-brain-smoke-v1"));
        assertTrue(report.toMarkdown().contains("Evidencia generada"));
        assertFalse(report.toMarkdown().contains("javafx"));
    }
}

package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class ReleaseCandidateDocumentationSourceTest {
    @Test
    void releaseCandidateDocumentationExists() throws Exception {
        assertContains("docs/testeo/SMOKE_MANUAL_RELEASE_CANDIDATE.md", "Word/DOCX");
        assertContains("docs/testeo/SMOKE_MANUAL_RELEASE_CANDIDATE.md", "guion narrable");
        assertContains("docs/testeo/SMOKE_MANUAL_RELEASE_CANDIDATE.md", "audio");
        assertContains("docs/testeo/SMOKE_MANUAL_RELEASE_CANDIDATE.md", "storyboard");
        assertContains("docs/testeo/reportes/REPORTE_RELEASE_CANDIDATE.md", "Resultado");
        assertContains("DOCUMENTACION/60_TANDA_15_IMPLEMENTADA_PACKAGING_RELEASE_CANDIDATE.md", "Tanda 15");
        assertContains("DOCUMENTACION_ESTRATEGICA/PLAN_IMPLEMENTACION_DETALLADO/TANDA_15_ESTADO_IMPLEMENTACION.md", "release candidate");
    }

    @Test
    void handoffMentionsCurrentFrontendPolishAndRemainingReleaseCandidatePath() throws Exception {
        String handoff = Files.readString(Path.of("AI_HANDOFF.md"));
        assertTrue(handoff.contains("Tanda 81D"));
        assertTrue(handoff.contains("T81E"));
        assertTrue(handoff.contains("Documento narrable"));
        assertTrue(handoff.contains("Word/PDF/TXT/Markdown") || handoff.contains("Word/DOCX"));
    }

    private static void assertContains(String path, String expected) throws Exception {
        assertTrue(Files.exists(Path.of(path)), path + " debe existir.");
        String text = Files.readString(Path.of(path));
        assertTrue(text.contains(expected), path + " debe contener: " + expected);
    }
}

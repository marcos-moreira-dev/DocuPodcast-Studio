package com.marcosmoreiradev.docupodcaststudio.application.brain;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BrainV1CapabilityMatrixTest {
    @Test
    void currentMatrixFreezesCoreCapabilitiesAndExplicitDeferrals() {
        BrainV1CapabilityMatrix matrix = BrainV1CapabilityMatrix.current();
        Set<String> ids = matrix.capabilities().stream().map(BrainV1Capability::id).collect(Collectors.toSet());

        assertTrue(ids.contains("document-intake"));
        assertTrue(ids.contains("listen-document"));
        assertTrue(ids.contains("narrative-layers"));
        assertTrue(ids.contains("media-input"));
        assertTrue(ids.contains("project-integrity"));
        assertTrue(ids.contains("export-readiness"));
        assertTrue(ids.contains("compute-device"));
        assertTrue(ids.contains("brain-smoke"));
        assertTrue(ids.contains("ocr"));
        assertTrue(ids.contains("advanced-video-editor"));
        assertFalse(matrix.availableInV1().isEmpty());
        assertFalse(matrix.deferredToV2().isEmpty());
    }

    @Test
    void matrixFindsCapabilitiesByStableId() {
        BrainV1CapabilityMatrix matrix = BrainV1CapabilityMatrix.current();
        BrainV1Capability media = matrix.findById(" MEDIA-INPUT ").orElseThrow();

        assertEquals(BrainV1CapabilityArea.MEDIA_ASSETS, media.area());
        assertTrue(media.userContract().contains("MP3"));
        assertTrue(media.userContract().contains("WAV"));
        assertTrue(media.limitation().contains("FFmpeg"));
    }

    @Test
    void markdownReportIsAuditableAndMentionsV1Boundaries() {
        String markdown = BrainV1CapabilityMatrix.current().toMarkdown();

        assertTrue(markdown.contains("Matriz congelada del cerebro V1"));
        assertTrue(markdown.contains("document-intake"));
        assertTrue(markdown.contains("V1_READY_WITH_LIMITS"));
        assertTrue(markdown.contains("V2_DEFERRED"));
        assertTrue(markdown.contains("PDF entra directamente como workspace V2 por páginas"));
        assertTrue(markdown.contains("Nube y colaboración"));
    }
}

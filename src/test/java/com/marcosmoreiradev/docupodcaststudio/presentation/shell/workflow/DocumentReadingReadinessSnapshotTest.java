package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentProcessingScope;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class DocumentReadingReadinessSnapshotTest {
    @Test
    void unpreparedFullDocumentUsesProcessLabel() {
        var snapshot = DocumentReadingReadinessSnapshot.unavailable("en");

        assertEquals("Procesar lectura completa", snapshot.completeReadingActionLabel());
        assertFalse(snapshot.semanticReady());
        assertFalse(snapshot.listeningReady());
    }

    @Test
    void semanticAndListeningReadyOffersToCompleteMissingAudioWithoutReprocessConfirmation() {
        var snapshot = new DocumentReadingReadinessSnapshot(
                DocumentProcessingScope.FULL_DOCUMENT, true, true, "en",
                List.of(), List.of(), 6, 11, 0, 0);

        assertEquals("Completar audio de lectura",
                snapshot.completeReadingActionLabel());
        assertFalse(snapshot.reprocessing());
        assertFalse(snapshot.audioReady());
    }

    @Test
    void completeAudioCoverageUsesReprocessLabel() {
        var snapshot = new DocumentReadingReadinessSnapshot(
                DocumentProcessingScope.FULL_DOCUMENT, true, true, "en",
                List.of(), List.of(), 17, 0, 0, 0);

        assertEquals("Reprocesar lectura completa", snapshot.completeReadingActionLabel());
        assertTrue(snapshot.reprocessing());
        assertTrue(snapshot.audioReady());
    }

    @Test
    void incompleteTranslationKeepsProcessLabel() {
        var snapshot = new DocumentReadingReadinessSnapshot(
                DocumentProcessingScope.FULL_DOCUMENT, true, false, "en",
                List.of("SEG-1"), List.of(), 0, 1, 0, 0);

        assertTrue(snapshot.semanticReady());
        assertFalse(snapshot.listeningReady());
        assertEquals("Procesar lectura completa", snapshot.completeReadingActionLabel());
    }
}

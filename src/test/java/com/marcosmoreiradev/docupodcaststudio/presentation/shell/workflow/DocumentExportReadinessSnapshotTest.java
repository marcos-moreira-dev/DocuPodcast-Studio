package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.application.document.ResolvedDocumentProcessingSelection;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentProcessingScope;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentProcessingIntervalUnit;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

final class DocumentExportReadinessSnapshotTest {
    @Test
    void missingTranslationsKeepPassivePreflightBlocked() {
        var snapshot = new DocumentExportReadinessSnapshot(
                "DOCEXP-CHECK-1", selection(), Optional.empty(),
                List.of("SEG-2", "SEG-3"), List.of(),
                List.of("SEG-2", "SEG-3"), List.of(),
                List.of(), List.of("SEG-2", "SEG-3"), false);

        assertFalse(snapshot.translationsReady());
        assertFalse(snapshot.audioReady());
        assertFalse(snapshot.readyToRender());
        assertEquals(2, snapshot.missingDerivativeCount());
        assertEquals(List.of(2, 3), snapshot.selection().resolvedPageNumbers());
    }

    @Test
    void fullyPersistedCoverageAllowsDirectRenderWithoutPreparation() {
        NarrationScriptDocument effective = new NarrationScriptDocument(
                "SCRIPT", "Document", "en", "source.pdf", List.of(),
                Instant.EPOCH, Instant.EPOCH, "");
        var snapshot = new DocumentExportReadinessSnapshot(
                "DOCEXP-CHECK-2", selection(), Optional.of(effective),
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), true);

        assertTrue(snapshot.translationsReady());
        assertTrue(snapshot.audioReady());
        assertTrue(snapshot.readyToRender());
        assertEquals(0, snapshot.missingDerivativeCount());
    }

    @Test
    void completeWavsWithMissingCompositionsAreNotReportedAsMissingAudio() {
        NarrationScriptDocument effective = new NarrationScriptDocument(
                "SCRIPT", "Document", "en", "source.pdf", List.of(),
                Instant.EPOCH, Instant.EPOCH, "");
        var snapshot = new DocumentExportReadinessSnapshot(
                "DOCEXP-CHECK-3", selection(), Optional.of(effective),
                List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of("SEG-2"), false);

        assertTrue(snapshot.audioReady());
        assertTrue(snapshot.compositionOnlyPending());
        assertEquals(DocumentExportReadinessSnapshot.ReadinessStage.COMPOSITION,
                snapshot.blockingStage());
        assertFalse(snapshot.readyToRender());
    }

    private static ResolvedDocumentProcessingSelection selection() {
        return new ResolvedDocumentProcessingSelection(
                DocumentProcessingScope.INTERVAL, DocumentProcessingIntervalUnit.PAGE,
                2, 3, List.of(2, 3),
                List.of("SEG-2", "SEG-3"), "SOURCE", "SHA", "REVISION",
                new NarrationScriptDocument(
                "SCRIPT", "Document", "es", "source.pdf", List.of(),
                Instant.EPOCH, Instant.EPOCH, ""));
    }
}

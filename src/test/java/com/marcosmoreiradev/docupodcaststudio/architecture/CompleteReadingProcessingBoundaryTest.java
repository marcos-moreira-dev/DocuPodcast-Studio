package com.marcosmoreiradev.docupodcaststudio.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

final class CompleteReadingProcessingBoundaryTest {
    @Test
    void reprocessConfirmationPrecedesOverlayAndCompleteProcessingDoesNotStartTts()
            throws Exception {
        String view = Files.readString(Path.of("src", "main", "java", "com",
                "marcosmoreiradev", "docupodcaststudio", "presentation", "shell",
                "DocuPodcastShellView.java"));
        int handler = view.indexOf("private void handleGenerateChunksFromStatusBar()");
        int confirmation = view.indexOf("reprocessCompleteReadingDialog.confirm(owner())", handler);
        int overlay = view.indexOf("processOverlayExpanded.set(true)", confirmation);
        int end = view.indexOf("private void cancelDocumentPreparationForRestart()", handler);
        String body = view.substring(handler, end);

        assertTrue(confirmation > handler);
        assertTrue(overlay > confirmation, "Cancelar debe producir cero mutaciones de UI/trabajo");
        assertTrue(body.contains("prepareCompleteReadingThenRun"));
        assertFalse(body.contains("prepareCompleteAudioThenRun"));
        assertFalse(body.contains("completeIncrementalPdfAudioPreparation"));
    }

    @Test
    void processCompleteDoesNotRequireAnAudioEngine() throws Exception {
        String action = Files.readString(Path.of("src", "main", "java", "com",
                "marcosmoreiradev", "docupodcaststudio", "presentation", "shell",
                "workflow", "DocumentAudioAction.java"));
        assertTrue(action.contains("PROCESS_COMPLETE(true, false)"));
    }
}

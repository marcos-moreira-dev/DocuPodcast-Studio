package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** PLAYBACK-SELECT1 keeps the selected chunk as the first generated/playable unit. */
class PlaybackSelect1SourceTest {
    @Test
    void documentPrimaryActionDoesNotFallBackToWholeDocumentWhenSelectionNeedsAudio() throws Exception {
        String source = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java");

        assertTrue(source.contains("submitAudioGenerationFromSegment"));
        assertTrue(source.contains("scriptStartingAt"));
        assertTrue(source.contains("pendingPlaybackStartSegmentId"));
        assertTrue(source.contains("Generando audio desde la selección"));
        assertTrue(source.contains("el primer fragmento preparado será el seleccionado"));
        assertFalse(source.contains("linked.ifPresent(segment -> selectedScriptSegmentId.set(segment.id()));\n        listenToDocument();"));
    }

    @Test
    void selectedGenerationUsesSuffixScriptAndRenderUnitPlan() throws Exception {
        String source = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java");
        String coordinator = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/AudioWorkflowCoordinator.java");

        assertTrue(source.contains("NarrationScriptDocument suffixScript"));
        assertTrue(coordinator.contains("dropWhile(segment -> !segment.id().equals(target))"));
        assertTrue(source.contains("renderUnitPlanStartingAt") || coordinator.contains("renderUnitPlanStartingAt"));
        assertTrue(coordinator.contains("allowedSegmentIds.contains(unit.segmentId())"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}

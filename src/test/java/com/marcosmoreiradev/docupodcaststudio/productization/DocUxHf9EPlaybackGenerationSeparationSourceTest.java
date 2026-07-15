package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** DOC-UX-HF9E: playback transport is separated from generation controls and sentence image sync. */
final class DocUxHf9EPlaybackGenerationSeparationSourceTest {
    @Test
    void playbarHasStartButtonAndDisablesPreviousNextAtDocumentBounds() throws Exception {
        String control = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/FloatingReadingControlBar.java"));
        String workspace = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java"));
        String vm = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java"));

        assertTrue(control.contains("onPlayFromBeginning"));
        assertTrue(control.contains("bindAvailability(previous, previousAvailable)"));
        assertTrue(control.contains("bindAvailability(next, nextAvailable)"));
        assertTrue(workspace.contains("viewModel::playDocumentFromBeginning"));
        assertTrue(workspace.contains("viewModel.previousFragmentAvailableProperty()"));
        assertTrue(workspace.contains("viewModel.nextFragmentAvailableProperty()"));
        assertTrue(vm.contains("startPlaybackFromCue(result.cue(), false)"));
        assertTrue(vm.contains("new PlaybackCursor(cue.segmentId(), cue.startSeconds(), false)"));
    }

    @Test
    void pauseAndStopPlaybackNoLongerCancelAudioGeneration() throws Exception {
        String vm = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java"));
        String pauseMethod = vm.substring(vm.indexOf("public void pausePlayback()"), vm.indexOf("public void resumePlayback()"));
        String stopMethod = vm.substring(vm.indexOf("public void stopPlayback()"), vm.indexOf("public void playDocumentFromBeginning()"));
        String status = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/status/StatusBarView.java"));

        assertFalse(pauseMethod.contains("cancelActiveAudioJobSilently"));
        assertFalse(stopMethod.contains("cancelActiveAudioJobSilently"));
        assertTrue(status.contains("Seguir generando"));
        assertTrue(status.contains("Cancelar generación"));
        assertTrue(status.contains("La reproducción se controla desde la barra flotante"));
    }

    @Test
    void unavailableEngineOpensHumanConfirmationBeforeGeneration() throws Exception {
        String shell = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java"));
        String vm = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java"));

        assertTrue(shell.contains("confirmAudioEngineReadyForDocumentAction"));
        assertTrue(shell.contains("Motor de voz no disponible"));
        assertTrue(shell.contains("handleOpenVoiceEngineSettings()"));
        assertTrue(vm.contains("audioEngineUnavailableForDocumentPrimaryAction"));
        assertTrue(vm.contains("audioEngineUnavailableForGeneration"));
    }

    @Test
    void leftImageInspectorResolvesExactSentenceRangeFromRailProjection() throws Exception {
        String vm = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java"));
        String panel = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentImageContextPanel.java"));

        assertTrue(vm.contains("sameDocumentRange(fragment, selectedRange)"));
        assertTrue(vm.contains("fragment.startOffset() == range.startOffset()"));
        assertTrue(vm.contains("fragment.endOffset() == range.endOffset()"));
        assertTrue(panel.contains("updatePreview(viewModel.selectedDocumentImageUri())"));
        assertTrue(panel.contains("selectedVisualFragmentKeyProperty()"));
    }
}

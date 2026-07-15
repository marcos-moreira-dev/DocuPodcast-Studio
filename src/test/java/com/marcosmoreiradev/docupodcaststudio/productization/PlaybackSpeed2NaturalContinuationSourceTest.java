package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** PLAYBACK-SPEED2: velocidad con tono natural y continuidad robusta entre chunks. */
final class PlaybackSpeed2NaturalContinuationSourceTest {
    @Test
    void javaSoundUsesPitchPreservingTimeStretchInsteadOfSampleRateShift() throws Exception {
        String player = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/playback/JavaSoundSegmentAudioPlayer.java"));
        String processor = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/playback/PcmTimeStretchProcessor.java"));

        assertTrue(player.contains("PcmTimeStretchProcessor.speedUpPreservePitch"));
        assertTrue(player.contains("tono natural"));
        assertFalse(player.contains("sourceFormat.getSampleRate() * normalizeRate"));
        assertTrue(processor.contains("overlapAdd"));
        assertTrue(processor.contains("bestCorrelationOffset"));
    }

    @Test
    void speedChangesKeepPlaybackTimerAliveAndContinuationExplicit() throws Exception {
        String vm = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java"));

        assertTrue(vm.contains("playbackTimer.play();"));
        assertTrue(vm.contains("if (transitionToCue(next.get()))"));
        assertTrue(vm.contains("waitForBufferedContinuation(completedCue)"));
        assertTrue(vm.contains("No se pudo continuar con el siguiente fragmento"));
    }

    @Test
    void selectionActionSaysContinueFromSelectionNotOnlySelectedSentence() throws Exception {
        String coordinator = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/DocumentSelectionCoordinator.java"));
        String details = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentContextDetailsPanel.java"));
        String floating = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/FloatingReadingControlBar.java"));

        assertTrue(coordinator.contains("Reproducir desde selección"));
        assertTrue(coordinator.contains("continúa con las siguientes oraciones"));
        assertTrue(details.contains("Reproducir fragmento (solo este)"));
        assertTrue(floating.contains("setMaxWidth(820)"));
    }
}

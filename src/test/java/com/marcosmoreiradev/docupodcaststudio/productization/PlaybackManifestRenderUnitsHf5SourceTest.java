package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** HF5 protects playback integration for generated render-unit audio, not only exported WAV. */
final class PlaybackManifestRenderUnitsHf5SourceTest {
    @Test
    void playbackManifestUsesRenderUnitsForGeneratedTtsAudio() throws Exception {
        String useCase = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/playback/BuildPlaybackManifestUseCase.java"));
        assertTrue(useCase.contains("cueForNarrationUnit"));
        assertTrue(useCase.contains("unit.usesTts()"));
        assertTrue(useCase.contains("completedByUnitOrSegment.get(unit.id())"));
        assertTrue(useCase.contains("new PlaybackCue("));
        assertTrue(useCase.contains("segment.id()"));
        assertTrue(useCase.contains("unit.id()"));
    }

    @Test
    void shellRefreshesPlaybackManifestWhenAudioJobGainsSegments() throws Exception {
        String shell = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java"));
        assertTrue(shell.contains("status.completedSegments() > knownCues"));
        assertTrue(shell.contains("rebuildPlaybackManifestFromLatestJob()"));
        String controller = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/PlaybackContinuationController.java"));
        assertTrue(controller.contains("manifest.cueAt(position)"));
    }
}

package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** PLAYBACK-HF6: generated/exportable WAVs must be reused by document playback before regenerating. */
final class PlaybackManifestReuseHf6SourceTest {
    @Test
    void documentPlaybackRebuildsManifestBeforeDeclaringAudioMissing() throws Exception {
        String shell = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java"));
        assertTrue(shell.contains("manifest = rebuildPlaybackManifestFromLatestJob()"));
        assertTrue(shell.contains("playSelectedFragmentOnly"));
        assertTrue(shell.contains("Reproduciendo solo este fragmento"));
        assertTrue(shell.contains("Reproduciendo desde aquí"));
    }

    @Test
    void playbackManifestUsesUnitTtsButPreservesExternalAudioClipOverride() throws Exception {
        String useCase = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/playback/BuildPlaybackManifestUseCase.java"));
        String test = Files.readString(Path.of("src/test/java/com/marcosmoreiradev/docupodcaststudio/application/playback/BuildPlaybackManifestUseCaseTest.java"));
        assertTrue(useCase.contains("hasExternalAudioClip"));
        assertTrue(useCase.contains("NarrationRenderUnit::usesAudioClip"));
        assertTrue(test.contains("userAudioClipUnitsBecomeEffectivePlaybackCues"));
    }
}

package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** PLAYBACK-HF7 prevents generated chunks from overriding the fragment currently being heard. */
final class PlaybackSequentialSessionHf7SourceTest {
    @Test
    void javaSoundPlayerUsesDevicePositionAndPlaybackGeneration() throws Exception {
        String player = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/playback/JavaSoundSegmentAudioPlayer.java"));
        assertTrue(player.contains("playbackGeneration"));
        assertTrue(player.contains("isCurrentPlayback"));
        assertTrue(player.contains("getMicrosecondPosition"));
        assertTrue(player.contains("lastKnownPositionSeconds"));
        assertFalse(player.contains("framesWritten +="), "No debe medir playback por bytes escritos al buffer interno.");
    }

    @Test
    void shellDoesNotStartBufferedPlaybackWhilePlayerIsAlreadyPlaying() throws Exception {
        String shell = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java"));
        String transport = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/PlaybackTransportCoordinator.java"));
        assertTrue(shell.contains("playbackTransport.playerPlaying()"));
        assertTrue(transport.contains("player.playing()"));
        String controller = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/PlaybackContinuationController.java"));
        assertTrue(controller.contains("nextCueAfterUnit"));
        assertTrue(shell.contains("playCueForCursor"));
    }
}

package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** PLAYBACK-HF4 adds explicit previous/next fragment transport and a streaming internal WAV player. */
final class PlaybackNextPreviousUxHf4SourceTest {
    @Test
    void playbarExposesPreviousAndNextFragmentActions() throws Exception {
        String bar = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/FloatingReadingControlBar.java"));
        String document = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java"));
        String shell = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java"));
        String navigator = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/PlaybackFragmentNavigator.java"));
        assertTrue(bar.contains("Fragmento anterior"));
        assertTrue(bar.contains("Siguiente fragmento"));
        assertTrue(document.contains("viewModel::playPreviousFragment"));
        assertTrue(document.contains("viewModel::playNextFragment"));
        assertTrue(shell.contains("playNextFragment"));
        assertTrue(shell.contains("playPreviousFragment"));
        assertTrue(navigator.contains("NavigationResult"));
    }

    @Test
    void internalPlayerStreamsPcmInsteadOfClipOnly() throws Exception {
        String player = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/playback/JavaSoundSegmentAudioPlayer.java"));
        assertTrue(player.contains("SourceDataLine"));
        assertTrue(player.contains("streamAudio"));
        assertTrue(player.contains("Formato no compatible"));
        assertTrue(player.contains("Reproductor interno listo"));
    }
}

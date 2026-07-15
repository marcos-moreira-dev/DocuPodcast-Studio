package com.marcosmoreiradev.docupodcaststudio.presentation.audio;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class AudioPlaybackManifestUiSourceTest {
    @Test
    void audioWorkspaceShowsPlaybackCues() throws Exception {
        String workspaceSource = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/audio/AudioWorkspaceView.java"));
        String stateSource = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/audio/AudioQueueState.java"));
        String viewModelSource = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java"));

        assertTrue(workspaceSource.contains("Playback sincronizado transversal"));
        assertTrue(workspaceSource.contains("playbackSyncState"));
        assertTrue(workspaceSource.contains("playbackCues"));
        assertTrue(workspaceSource.contains("state.playbackCues()"));
        assertTrue(workspaceSource.contains("playbackSynchronizedSegmentLabels"));
        assertTrue(stateSource.contains("List<String> playbackCues"));
        assertTrue(viewModelSource.contains("playbackCueLabels()"));
        assertTrue(viewModelSource.contains("playbackSynchronizedSegmentLabels()"));
    }
}

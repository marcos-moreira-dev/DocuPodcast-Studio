package com.marcosmoreiradev.docupodcaststudio.presentation.playback;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class PlaybackCrossWorkspaceSourceTest {
    @Test
    void viewModelExposesOneSynchronizationProjectionForAllWorkspaces() throws Exception {
        String viewModel = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java"));
        String script = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/script/ScriptWorkspaceView.java"));
        String audio = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/audio/AudioWorkspaceView.java"));
        String storyboard = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/storyboard/StoryboardWorkspaceView.java"));

        assertTrue(viewModel.contains("PlaybackSyncState.from"));
        assertTrue(viewModel.contains("playFromSegment"));
        assertTrue(viewModel.contains("selectedScriptSegmentId.set(nextCursor.segmentId())"));
        assertTrue(script.contains("playbackSyncState"));
        assertTrue(audio.contains("playbackSyncState"));
        assertTrue(storyboard.contains("playbackSyncState"));
    }
}

package com.marcosmoreiradev.docupodcaststudio.presentation.storyboard;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class StoryboardPlaybackSyncSourceTest {
    @Test
    void storyboardHighlightsActivePlaybackScene() throws Exception {
        String workspaceSource = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/storyboard/StoryboardWorkspaceView.java"));
        String presentationSource = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/storyboard/StoryboardScenePresentation.java"));
        String cssSource = Files.readString(Path.of("src/main/resources/css/storyboard.css"));

        assertTrue(workspaceSource.contains("playbackCursorProperty"));
        assertTrue(workspaceSource.contains("currentPlaybackManifestProperty"));
        assertTrue(workspaceSource.contains("scene.cardStateCssClass()"));
        assertTrue(presentationSource.contains("cardStateCssClass"));
        assertTrue(presentationSource.contains("storyboard-scene-active"));
        assertTrue(cssSource.contains("storyboard-scene-active"));
    }
}

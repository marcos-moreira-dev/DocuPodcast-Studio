package com.marcosmoreiradev.docupodcaststudio.presentation.storyboard;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class StoryboardWorkspaceSourceTest {
    @Test
    void storyboardWorkspaceExposesImageBindingAndPlaybackLanguage() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/storyboard/StoryboardWorkspaceView.java"));

        assertTrue(source.contains("Secuencia visual interna"));
        assertTrue(source.contains("Importar imagen"));
        assertTrue(source.contains("Asociar imagen"));
        assertTrue(source.contains("Play desde selección"));
        assertTrue(source.contains("playbackSyncState"));
        assertTrue(source.contains("playFromSegment"));
        assertTrue(source.contains("StoryboardScenePresentation"));
        assertTrue(source.contains("storyboard-scene-board"));
        assertTrue(source.contains("storyboard-thumbnail"));
        assertTrue(source.contains("Asociar última imagen"));
    }
}

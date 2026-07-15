package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class SourceVisualBlockFullscreenSourceTest {
    @Test
    void embeddedSourceImagesExposeFullscreenActionWithoutTempFiles() throws Exception {
        String block = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/SourceVisualBlockView.java"));
        String viewer = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/ImageFullscreenViewer.java"));

        assertTrue(block.contains("AppIcon.FULLSCREEN"));
        assertTrue(block.contains("openFullscreenImage(view, image, title, playbackRunning, pausePlayback, resumePlayback)"));
        assertTrue(block.contains("Pantalla completa (se pausa la reproduccion)"));
        assertTrue(viewer.contains("public static void show(\n            Image image"));
    }
}

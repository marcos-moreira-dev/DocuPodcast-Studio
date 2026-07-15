package com.marcosmoreiradev.docupodcaststudio.presentation.shell;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class StageDecorationSourceTest {
    @Test
    void appShouldRequestDecoratedNativeWindow() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/DocuPodcastStudioApp.java"));
        assertTrue(source.contains("StageStyle.DECORATED"));
        assertTrue(source.contains("stage.initStyle(StageStyle.DECORATED)"));
    }
}

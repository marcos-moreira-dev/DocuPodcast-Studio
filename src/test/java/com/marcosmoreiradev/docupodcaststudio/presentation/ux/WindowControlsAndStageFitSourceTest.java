package com.marcosmoreiradev.docupodcaststudio.presentation.ux;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WindowControlsAndStageFitSourceTest {
    @Test
    void shellShouldUseOnlyNativeWindowChromeAndStageShouldFitScreen() throws Exception {
        String shell = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java"));
        assertFalse(shell.contains("buildWindowControlStrip"));
        assertFalse(shell.contains("window-control-strip"));
        assertFalse(shell.contains("window-control-button"));
        assertFalse(shell.contains("Maximizar o restaurar"));

        String shellCss = Files.readString(Path.of("src/main/resources/css/shell.css"));
        assertFalse(shellCss.contains("window-control-strip"));
        assertFalse(shellCss.contains("window-control-close"));

        String app = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/DocuPodcastStudioApp.java"));
        assertTrue(app.contains("StageStyle.DECORATED"));
        assertTrue(app.contains("Screen.getPrimary().getVisualBounds()"));
        assertTrue(app.contains("fitStageInsideVisibleScreen"));
        assertTrue(app.contains("stage.centerOnScreen()"));
    }
}

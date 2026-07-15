package com.marcosmoreiradev.docupodcaststudio.presentation.ux;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class VisualFeedbackCssSourceTest {
    @Test
    void cssShouldKeepStatusFeedbackWelcomeAndNoDuplicateWindowControls() throws Exception {
        String shellCss = Files.readString(Path.of("src/main/resources/css/shell.css"));
        String statusCss = Files.readString(Path.of("src/main/resources/css/statusbar.css"));
        String welcomeCss = Files.readString(Path.of("src/main/resources/css/welcome.css"));
        String tokensCss = Files.readString(Path.of("src/main/resources/css/tokens.css"));
        assertFalse(shellCss.contains("window-control-strip"));
        assertFalse(shellCss.contains("window-control-close"));
        assertTrue(statusCss.contains("status-bar-prefix"));
        assertTrue(welcomeCss.contains("linear-gradient") || welcomeCss.contains("radial-gradient"));
        assertTrue(tokensCss.contains("dropshadow"));
        assertTrue(shellCss.contains("app-menu-bar > .container > .menu-button > .label"));
        assertTrue(shellCss.contains("app-menu-bar .context-menu .label"));
    }
}

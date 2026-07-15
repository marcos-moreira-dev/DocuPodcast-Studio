package com.marcosmoreiradev.docupodcaststudio.presentation.ux;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ModernVisualThemeSourceTest {
    @Test
    void modernThemeUsesFormalTeamsLikeTokensWithoutCircusUi() throws Exception {
        String tokens = Files.readString(Path.of("src/main/resources/css/tokens.css"));
        String shell = Files.readString(Path.of("src/main/resources/css/shell.css"));
        String toolbar = Files.readString(Path.of("src/main/resources/css/toolbar.css"));
        String ribbon = Files.readString(Path.of("src/main/resources/css/components/ribbon.css"));
        String welcome = Files.readString(Path.of("src/main/resources/css/welcome.css"));

        assertTrue(tokens.contains("Tanda 50 — modern visual tokens"));
        assertTrue(tokens.contains("-docu-accent: #5B5FC7"));
        assertTrue(tokens.contains("-docu-bg-chrome-dark"));
        assertTrue(tokens.contains("-docu-shadow-card"));
        assertTrue(shell.contains("modern shell chrome"));
        assertTrue(shell.contains("-docu-bg-chrome-dark"));
        assertTrue(toolbar.contains("no XP gradients"));
        assertTrue(ribbon.contains("docupodcast ribbon") || ribbon.contains("breathable desktop ribbon"));
        assertTrue(ribbon.contains("-fx-background-color: -docu-bg-chrome"));
        assertTrue(welcome.contains("desktop app start page") || welcome.contains("welcome-shell"));
        assertFalse(toolbar.contains("linear-gradient"));
        assertFalse(shell.contains("linear-gradient"));
    }
}

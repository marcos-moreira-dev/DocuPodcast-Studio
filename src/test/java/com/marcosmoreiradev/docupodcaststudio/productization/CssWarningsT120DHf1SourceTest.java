package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** T120D-HF1 prevents JavaFX CSS warnings seen when opening examples/settings surfaces. */
final class CssWarningsT120DHf1SourceTest {
    @Test
    void tokensDeclareAliasesUsedByExamplesAndSettings() throws Exception {
        String tokens = Files.readString(Path.of("src/main/resources/css/tokens.css"));
        String examples = Files.readString(Path.of("src/main/resources/css/examples/examples.css"));
        String settings = Files.readString(Path.of("src/main/resources/css/components/settings-shell.css"));

        assertTrue(tokens.contains("-dp-surface:"));
        assertTrue(tokens.contains("-dp-border:"));
        assertTrue(tokens.contains("-dp-primary:"));
        assertTrue(tokens.contains("-dp-primary-soft:"));
        assertTrue(tokens.contains("-dp-text:"));
        assertTrue(tokens.contains("-dp-text-secondary:"));
        assertTrue(tokens.contains("-dp-muted:"));
        assertTrue(tokens.contains("-docu-chip-background:"));

        assertTrue(examples.contains("-fx-background-color: -dp-surface;"));
        assertTrue(examples.contains("-fx-border-color: -dp-border;"));
        assertTrue(examples.contains("-fx-text-fill: -dp-text;"));
        assertTrue(settings.contains("-fx-background-color: -docu-chip-background;"));
    }

    @Test
    void knownConsoleWarningTokensAreNotLeftOrphaned() throws Exception {
        String tokens = Files.readString(Path.of("src/main/resources/css/tokens.css"));
        String allCss = Files.readString(Path.of("src/main/resources/css/docupodcast-light.css"))
                + Files.readString(Path.of("src/main/resources/css/examples/examples.css"))
                + Files.readString(Path.of("src/main/resources/css/components/settings-shell.css"));

        for (String alias : new String[]{"-dp-text", "-dp-text-secondary", "-dp-primary", "-dp-muted", "-dp-surface", "-dp-border", "-docu-chip-background"}) {
            assertTrue(tokens.contains(alias + ":"), "Missing CSS alias: " + alias);
        }

        assertFalse(allCss.contains("PATH global"), "Smoke-facing CSS/docs should avoid global-path wording regressions.");
    }
}

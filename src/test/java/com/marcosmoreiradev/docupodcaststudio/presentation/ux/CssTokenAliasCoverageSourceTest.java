package com.marcosmoreiradev.docupodcaststudio.presentation.ux;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class CssTokenAliasCoverageSourceTest {
    @Test
    void compatibilityAliasesCoverTokensSeenInSmokeWarnings() throws Exception {
        String tokens = Files.readString(Path.of("src/main/resources/css/tokens.css"));
        String documentPage = Files.readString(Path.of("src/main/resources/css/document/document-page.css"));
        String audioJobs = Files.readString(Path.of("src/main/resources/css/audio-jobs.css"));
        String renderProgress = Files.readString(Path.of("src/main/resources/css/components/render-progress.css"));

        assertTrue(tokens.contains("-dp-muted-text"));
        assertTrue(tokens.contains("-docu-accent-dark"));
        assertTrue(tokens.contains("-docu-bg-shell"));
        assertTrue(tokens.contains("-docu-bg-subtle"));
        assertTrue(tokens.contains("-docu-bg-soft"));
        assertTrue(tokens.contains("-docu-surface"));
        assertTrue(tokens.contains("-docu-surface-muted"));
        assertTrue(tokens.contains("-docu-ink"));
        assertTrue(tokens.contains("-docu-muted"));
        assertTrue(tokens.contains("-docu-soft"));
        assertTrue(tokens.contains("-docu-shadow"));

        assertTrue(documentPage.contains("-docu-bg-subtle"));
        assertTrue(documentPage.contains("-dp-muted-text"));
        assertTrue(audioJobs.contains("-docu-accent-dark"));
        assertTrue(renderProgress.contains("-docu-surface"));
    }
}

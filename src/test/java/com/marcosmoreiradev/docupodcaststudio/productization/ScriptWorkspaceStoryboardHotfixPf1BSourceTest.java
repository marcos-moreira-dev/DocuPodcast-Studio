package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** PF1B guardrail: Script workspace must follow the current Storyboard naming/API. */
final class ScriptWorkspaceStoryboardHotfixPf1BSourceTest {
    @Test
    void scriptWorkspaceDoesNotUseRemovedVisualesApi() throws Exception {
        String view = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/script/ScriptWorkspaceView.java"));
        String shell = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java"));
        String storyboard = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/domain/storyboard/StoryboardDocument.java"));

        assertTrue(view.contains("import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardDocument;"));
        assertTrue(view.contains("currentStoryboardProperty()"));
        assertTrue(view.contains("StoryboardDocument storyboard"));
        assertTrue(shell.contains("currentStoryboardProperty()"));
        assertTrue(storyboard.contains("public record StoryboardDocument"));
        assertFalse(view.contains("VisualesDocument"));
        assertFalse(view.contains("currentVisualesProperty"));
    }
}

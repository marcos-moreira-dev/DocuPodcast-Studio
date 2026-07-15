package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** DEMO-FALLBACK-NOTICES-HF1 makes partial demo visual binding visible to the user. */
final class DemoFallbackNoticesHf1SourceTest {
    @Test
    void skippedVisualBindingsProduceDialogDecision() throws Exception {
        String visualWorkflow = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/ExampleVisualBindingWorkflow.java");
        String creationWorkflow = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/ExampleProjectCreationWorkflow.java");
        String shell = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java");

        assertTrue(visualWorkflow.contains("hasFallbacks"));
        assertTrue(visualWorkflow.contains("UserVisibleDecision.defensiveFallback"));
        assertTrue(visualWorkflow.contains("sin asociar"));
        assertTrue(creationWorkflow.contains("visualResult.userDecision"));
        assertTrue(shell.contains("alertPresenter.showDecision"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}

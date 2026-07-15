package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** EXAMPLE-WORKFLOW-RF1 keeps demo creation orchestration out of the JavaFX shell view. */
final class ExampleWorkflowRf1SourceTest {
    @Test
    void shellDelegatesExampleProjectCreationToWorkflow() throws Exception {
        String shell = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java");
        String workflow = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/ExampleProjectCreationWorkflow.java");

        assertTrue(shell.contains("ExampleProjectCreationWorkflow"));
        assertTrue(shell.contains("exampleProjectCreationWorkflow.create"));
        assertFalse(shell.contains("createExampleProject().materialize"));
        assertTrue(workflow.contains("createExampleProject()"));
        assertTrue(workflow.contains("importWordDocument"));
        assertTrue(workflow.contains("saveCurrentProjectAs"));
        assertTrue(workflow.contains("importAndBindExampleVisuals"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}

package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TheatreFrameGenerationWorkflowSourceTest {
    @Test
    void intermediateFrameRequestsUseOnlyPreviousAndNextFrameReferences() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/TheatreFrameGenerationWorkflow.java"));

        assertTrue(source.contains("VisualConditioningRole.PREVIOUS_FRAME"));
        assertTrue(source.contains("VisualConditioningRole.NEXT_FRAME"));
        assertTrue(source.contains("transitionReferences("));
        assertTrue(source.contains("TheatreImageGenerationWorkflow.visualEngineRequest(unit, preset"));
        assertTrue(source.contains(".withConditioningReferences(transitionReferences)"));
        assertTrue(source.contains("null);"));
        assertFalse(source.contains("current.contextPackage())"));
        assertFalse(source.contains("Contexto espacial inicial"));
    }
}

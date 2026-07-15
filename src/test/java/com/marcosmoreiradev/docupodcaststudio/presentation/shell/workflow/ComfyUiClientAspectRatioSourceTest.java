package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ComfyUiClientAspectRatioSourceTest {
    @Test
    void workflowUsesSelectedAspectRatioForPromptAndDimensions() throws Exception {
        String client = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/visual/ComfyUiVisualEngineClient.java"));
        String payloadFactory = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/visual/ComfyUiWorkflowPayloadFactory.java"));
        String workflow = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/TheatreImageGenerationWorkflow.java"));

        assertTrue(workflow.contains("TheatreImageAspectRatio"));
        assertTrue(workflow.contains("aspectRatio.promptText()") || workflow.contains("selectedAspect.promptText()"));
        assertTrue(workflow.contains("selectedAspect.widthFor(selectedProfile)"));
        assertTrue(workflow.contains("selectedAspect.heightFor(selectedProfile)"));
        assertTrue(client.contains("currentWorkflow.generationWidth(request)"));
        assertTrue(client.contains("currentWorkflow.generationHeight(request)"));
        assertTrue(payloadFactory.contains("request.generationWidth()"));
        assertTrue(payloadFactory.contains("workflow.generationWidth(request)"));
        assertFalse(workflow.contains("16:9 composition, consistent characters"));
    }
}

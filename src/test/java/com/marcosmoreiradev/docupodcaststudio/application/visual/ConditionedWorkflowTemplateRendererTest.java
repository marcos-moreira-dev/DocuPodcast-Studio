package com.marcosmoreiradev.docupodcaststudio.application.visual;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ConditionedWorkflowTemplateRendererTest {
    @TempDir
    Path temp;

    @Test
    void rendersIdentityAndStructureReferencesIntoVerifiedApiWorkflow() throws Exception {
        Path template = temp.resolve("workflow.json");
        Files.writeString(template, """
                {"prompt":{"1":{"inputs":{"text":"{{PROMPT}}","image":"{{IDENTITY_IMAGE_1}}",
                "identity_strength":{{IDENTITY_IMAGE_1_STRENGTH}},"sketch":"{{STRUCTURE_GUIDE}}",
                "sketch_strength":{{STRUCTURE_GUIDE_STRENGTH}},"width":{{WIDTH}},"height":{{HEIGHT}},
                "prefix":"{{FILENAME_PREFIX}}"}}}}
                """);
        VisualConditioningReference identity = new VisualConditioningReference(
                "PERSON-1", "Capitan", temp.resolve("capitan.png"), VisualConditioningRole.IDENTITY, 0.85,
                "docupodcast/capitan.png");
        VisualConditioningReference sketch = new VisualConditioningReference(
                "FRAME-1", "Boceto", temp.resolve("boceto.png"), VisualConditioningRole.STRUCTURE_GUIDE, 0.30,
                "docupodcast/boceto.png");
        VisualEngineRequest request = new VisualEngineRequest(
                "escena con dos aviadores", "", "sd15.safetensors", 20, 7.0, 1,
                512, 512, temp, "frame-1", List.of(identity, sketch));

        String rendered = new ConditionedWorkflowTemplateRenderer().render(
                template, request, ComfyUiWorkflowSpec.sd15());

        assertTrue(rendered.contains("docupodcast/capitan.png"));
        assertTrue(rendered.contains("docupodcast/boceto.png"));
        assertTrue(rendered.contains("\"identity_strength\":0.85"));
        assertTrue(rendered.contains("\"sketch_strength\":0.3"));
        assertFalse(rendered.contains("{{"));
    }

    @Test
    void rejectsReferenceThatWasNotUploadedToComfyUi() throws Exception {
        Path template = temp.resolve("workflow.json");
        Files.writeString(template, "{\"prompt\":{\"image\":\"{{IDENTITY_IMAGE_1}}\"}}");
        VisualEngineRequest request = new VisualEngineRequest(
                "escena", "", "sd15.safetensors", 20, 7.0, 1,
                512, 512, temp, "frame",
                List.of(new VisualConditioningReference("P1", "Personaje", temp.resolve("personaje.png"),
                        VisualConditioningRole.IDENTITY, 0.85)));

        IOException error = assertThrows(IOException.class, () -> new ConditionedWorkflowTemplateRenderer()
                .render(template, request, ComfyUiWorkflowSpec.sd15()));

        assertTrue(error.getMessage().contains("no fue subida"));
    }
}

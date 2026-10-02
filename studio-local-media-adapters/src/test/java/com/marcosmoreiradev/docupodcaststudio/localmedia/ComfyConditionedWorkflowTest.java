package com.marcosmoreiradev.docupodcaststudio.localmedia;

import com.marcosmoreiradev.docupodcaststudio.media.api.MediaReference;
import com.marcosmoreiradev.docupodcaststudio.media.api.MediaReferenceRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ComfyConditionedWorkflowTest {
    @TempDir Path temporary;

    @Test
    void buildsIndependentRegionalIdentityMasksAndRasterImg2Img() throws Exception {
        var references = List.of(
                uploaded("bigote-frontal", MediaReferenceRole.REGIONAL_IDENTITY, "CAPITAN-BIGOTE",
                        0.08, 0.08, 0.34, 0.84),
                uploaded("bigote-lateral", MediaReferenceRole.REGIONAL_IDENTITY, "CAPITAN-BIGOTE",
                        0.08, 0.08, 0.34, 0.84),
                uploaded("tornillo-frontal", MediaReferenceRole.REGIONAL_IDENTITY, "TENIENTE-TORNILLO",
                        0.58, 0.08, 0.34, 0.84),
                uploaded("storyboard", MediaReferenceRole.COMPOSITION_GUIDE, "", 0, 0, 1, 1));

        String workflow = new ComfyConditionedWorkflow().render(
                "sd15.safetensors", "scribble.safetensors",
                "hangar con dos aviadores", "identidades mezcladas",
                960, 544, 42L, 24, 6.0, "intervencion-2", references);

        assertTrue(workflow.contains("\"class_type\":\"IPAdapterUnifiedLoader\""));
        assertTrue(count(workflow, "\"class_type\":\"IPAdapterAdvanced\"") >= 2);
        assertTrue(count(workflow, "\"class_type\":\"MaskComposite\"") == 2);
        assertTrue(count(workflow, "\"class_type\":\"ConditioningSetMask\"") == 2);
        assertTrue(workflow.contains("\"combine_embeds\":\"average\""));
        assertFalse(workflow.contains("\"class_type\":\"ImageBatch\""));
        assertTrue(workflow.contains("\"class_type\":\"VAEEncode\""));
        assertTrue(workflow.contains("\"denoise\":0.2"));
        assertTrue(workflow.contains("\"weight_type\":\"style transfer precise\""));
        assertTrue(workflow.contains("\"end_at\":0.82"));
        assertFalse(workflow.contains("\"class_type\":\"ControlNetLoader\""));
        assertFalse(workflow.contains("\"class_type\":\"EmptyLatentImage\""));
    }

    @Test
    void usesControlNetScribbleForDrawnActiveStoryboard() throws Exception {
        var references = List.of(
                uploaded("actor", MediaReferenceRole.REGIONAL_IDENTITY, "ACTOR",
                        0.25, 0.08, 0.50, 0.84),
                uploaded("drawn-frame", MediaReferenceRole.DRAWN_GUIDE, "", 0, 0, 1, 1));

        String workflow = new ComfyConditionedWorkflow().render(
                "sd15.safetensors", "control_v11p_sd15_scribble_fp16.safetensors",
                "escena", "defectos", 960, 544, 7L, 24, 6.0,
                "drawn", references);

        assertTrue(workflow.contains("\"class_type\":\"ControlNetLoader\""));
        assertTrue(workflow.contains("control_v11p_sd15_scribble_fp16.safetensors"));
        assertTrue(workflow.contains("\"class_type\":\"ControlNetApplyAdvanced\""));
        assertTrue(workflow.contains("\"strength\":0.78"));
        assertTrue(workflow.contains("\"class_type\":\"EmptyLatentImage\""));
        assertFalse(workflow.contains("\"class_type\":\"VAEEncode\""));
    }

    private ComfyConditionedWorkflow.Uploaded uploaded(
            String id,
            MediaReferenceRole role,
            String subject,
            double x,
            double y,
            double width,
            double height) throws Exception {
        Path image = Files.write(temporary.resolve(id + ".png"), new byte[]{1, 2, 3});
        Map<String, String> metadata = role.equals(MediaReferenceRole.REGIONAL_IDENTITY)
                ? Map.of(
                "subjectId", subject,
                "subjectName", subject,
                "regionX", Double.toString(x),
                "regionY", Double.toString(y),
                "regionWidth", Double.toString(width),
                "regionHeight", Double.toString(height))
                : Map.of();
        return new ComfyConditionedWorkflow.Uploaded(
                new MediaReference(id, image, role, 0.82, metadata),
                "uploaded-" + id + ".png");
    }

    private static int count(String source, String token) {
        int result = 0;
        int offset = 0;
        while ((offset = source.indexOf(token, offset)) >= 0) {
            result++;
            offset += token.length();
        }
        return result;
    }
}

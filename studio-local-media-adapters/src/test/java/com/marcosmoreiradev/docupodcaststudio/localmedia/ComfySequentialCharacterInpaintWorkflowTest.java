package com.marcosmoreiradev.docupodcaststudio.localmedia;

import com.marcosmoreiradev.docupodcaststudio.media.api.MediaReference;
import com.marcosmoreiradev.docupodcaststudio.media.api.MediaReferenceRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ComfySequentialCharacterInpaintWorkflowTest {
    @TempDir Path temporary;

    @Test
    void insertsEachCharacterOnceWithIndependentMaskedImg2ImgPasses() throws Exception {
        var references = List.of(
                uploaded("bigote-frontal", "CAPITAN-BIGOTE", 0.10),
                uploaded("bigote-lateral", "CAPITAN-BIGOTE", 0.10),
                uploaded("tornillo-frontal", "TENIENTE-TORNILLO", 0.60),
                uploaded("tornillo-lateral", "TENIENTE-TORNILLO", 0.60));

        String workflow = new ComfySequentialCharacterInpaintWorkflow().render(
                "DreamShaper_8_pruned.safetensors",
                "hangar teatral con biplano",
                "personajes duplicados",
                960,
                544,
                42L,
                28,
                "intervencion-2-personajes",
                "base.png",
                references);

        assertEquals(2, count(workflow, "\"class_type\":\"IPAdapterAdvanced\""));
        assertEquals(2, count(workflow, "\"class_type\":\"MaskComposite\""));
        assertEquals(2, count(workflow, "\"class_type\":\"FeatherMask\""));
        assertEquals(2, count(workflow, "\"class_type\":\"VAEEncodeForInpaint\""));
        assertEquals(2, count(workflow, "\"class_type\":\"KSampler\""));
        assertEquals(2, count(workflow, "\"class_type\":\"VAEDecode\""));
        assertEquals(3, count(workflow, "\"class_type\":\"LoadImage\""));
        assertTrue(workflow.contains("\"attn_mask\""));
        assertTrue(workflow.contains("\"denoise\":0.82"));
        assertTrue(workflow.contains("Exactly one person inside the active region"));
        assertFalse(workflow.contains("\"class_type\":\"EmptyLatentImage\""));
        assertFalse(workflow.contains("uploaded-bigote-lateral.png"));
        assertFalse(workflow.contains("uploaded-tornillo-lateral.png"));
    }

    private ComfyConditionedWorkflow.Uploaded uploaded(
            String id,
            String subject,
            double x) throws Exception {
        Path image = Files.write(temporary.resolve(id + ".png"), new byte[]{1, 2, 3});
        return new ComfyConditionedWorkflow.Uploaded(
                new MediaReference(
                        id,
                        image,
                        MediaReferenceRole.REGIONAL_IDENTITY,
                        0.82,
                        Map.of(
                                "subjectId", subject,
                                "subjectName", subject,
                                "regionX", Double.toString(x),
                                "regionY", "0.08",
                                "regionWidth", "0.30",
                                "regionHeight", "0.84")),
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

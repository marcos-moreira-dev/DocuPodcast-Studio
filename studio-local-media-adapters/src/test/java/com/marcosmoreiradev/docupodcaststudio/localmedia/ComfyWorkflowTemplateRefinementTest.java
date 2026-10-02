package com.marcosmoreiradev.docupodcaststudio.localmedia;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComfyWorkflowTemplateRefinementTest {
    @Test
    void usesTheExistingImageAsLatentAndAppliesControlNetTile() {
        String workflow = ComfyWorkflowTemplate.refinement(
                "entrada-ñ.png",
                "v1-5-pruned-emaonly-fp16.safetensors",
                "control_v11f1e_sd15_tile.pth",
                "preservar composición",
                "deformaciones",
                "salida",
                42L,
                12,
                0.18,
                0.95,
                5.5);

        assertTrue(workflow.contains("\"class_type\":\"LoadImage\""));
        assertTrue(workflow.contains("\"class_type\":\"ControlNetLoader\""));
        assertTrue(workflow.contains("\"class_type\":\"ControlNetApplyAdvanced\""));
        assertTrue(workflow.contains("\"class_type\":\"VAEEncode\""));
        assertTrue(workflow.contains("\"class_type\":\"KSampler\""));
        assertTrue(workflow.contains("\"denoise\":0.18"));
        assertTrue(workflow.contains("\"strength\":0.95"));
        assertTrue(workflow.contains("\"latent_image\":[\"7\",0]"));
        assertFalse(workflow.contains("EmptyLatentImage"));
        assertFalse(workflow.contains("UpscaleModelLoader"));
    }
}

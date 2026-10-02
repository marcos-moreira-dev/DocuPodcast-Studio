package com.marcosmoreiradev.docupodcaststudio.localmedia;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ComfyWorkflowTemplateSuperResolutionTest {
    @Test
    void dedicatedWorkflowNeverContainsDiffusionGenerationNodes() {
        String workflow = ComfyWorkflowTemplate.superResolution(
                "entrada-ñ.png", "RealESRGAN_x4plus.pth", "salida");

        assertTrue(workflow.contains("\"class_type\":\"LoadImage\""));
        assertTrue(workflow.contains("\"class_type\":\"UpscaleModelLoader\""));
        assertTrue(workflow.contains("\"class_type\":\"ImageUpscaleWithModel\""));
        assertTrue(workflow.contains("\"class_type\":\"SaveImage\""));
        assertFalse(workflow.contains("KSampler"));
        assertFalse(workflow.contains("CheckpointLoader"));
        assertFalse(workflow.contains("CLIPTextEncode"));
        assertFalse(workflow.contains("EmptyLatentImage"));
    }
}

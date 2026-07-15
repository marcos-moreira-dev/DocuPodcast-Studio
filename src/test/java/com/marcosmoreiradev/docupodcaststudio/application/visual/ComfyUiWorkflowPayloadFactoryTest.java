package com.marcosmoreiradev.docupodcaststudio.application.visual;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ComfyUiWorkflowPayloadFactoryTest {
    @Test
    void fluxPayloadUsesComponentNodesCpuTextEncodersAndNativeWideGeometry() throws Exception {
        VisualEngineRequest request = new VisualEngineRequest(
                "alien corriendo en la piramide de egipto", "ignored", "flux1-dev.safetensors",
                24, 3.5, 1, 1920, 1080, Path.of("generated"), "flux-smoke");
        ComfyUiWorkflowSpec workflow = ComfyUiWorkflowSpec.flux(
                "flux1-dev.safetensors", "ae.safetensors", "clip_l.safetensors",
                "t5xxl_bf16.safetensors", 1344, 768);

        String payload = new ComfyUiWorkflowPayloadFactory().create(request, workflow);

        assertTrue(payload.contains("\"class_type\":\"UNETLoader\""));
        assertTrue(payload.contains("\"class_type\":\"DualCLIPLoader\""));
        assertTrue(payload.contains("\"device\":\"cpu\""));
        assertTrue(payload.contains("\"class_type\":\"CLIPTextEncodeFlux\""));
        assertTrue(payload.contains("\"class_type\":\"SamplerCustomAdvanced\""));
        assertTrue(payload.contains("\"width\":1344"));
        assertTrue(payload.contains("\"height\":768"));
        assertTrue(payload.contains("alien corriendo en la piramide de egipto"));
        assertFalse(payload.contains("negative"));
    }

    @Test
    void sd15PayloadKeepsCheckpointWorkflow() throws Exception {
        VisualEngineRequest request = new VisualEngineRequest(
                "stage", "bad", "sd15.safetensors", 20, 7.0, 1,
                512, 512, Path.of("generated"), "sd15");

        String payload = new ComfyUiWorkflowPayloadFactory().create(request, ComfyUiWorkflowSpec.sd15());

        assertTrue(payload.contains("CheckpointLoaderSimple"));
        assertTrue(payload.contains("KSampler"));
        assertTrue(payload.contains("sd15.safetensors"));
    }
}

package com.marcosmoreiradev.docupodcaststudio.application.modelsetup;

import com.marcosmoreiradev.docupodcaststudio.application.visual.VisualGenerationProfile;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ImageEnginePresetSupportPolicyTest {
    @Test
    void sd15PresetUsesBuiltInReferenceWorkflow() {
        ImageEnginePresetSupport support = ImageEnginePresetSupportPolicy.forProfile(
                VisualGenerationProfile.DIAGNOSTIC_SD15);

        assertTrue(support.builtInWorkflowAvailable());
        assertEquals("v1-5-pruned-emaonly-fp16.safetensors", support.checkpointName());
        assertEquals("workflows/workflow-sd15-reference.json", support.workflowName());
        assertTrue(support.diagnostic().contains("builtInWorkflow=true"));
    }

    @Test
    void fluxPresetUsesIntegratedComponentWorkflow() {
        ImageEnginePresetSupport support = ImageEnginePresetSupportPolicy.forPresetId("HIGH_QUALITY_FLUX");

        assertTrue(support.builtInWorkflowAvailable());
        assertEquals("flux1-dev.safetensors", support.checkpointName());
        assertEquals("workflows/workflow-flux-reference.json", support.workflowName());
        assertTrue(support.userMessage().contains("workflow integrado por componentes"));
        assertTrue(support.userMessage().contains("CLIP-L"));
        assertTrue(support.diagnostic().contains("builtInWorkflow=true"));
    }

    @Test
    void customWorkflowRequiresExplicitImportedComfyWorkflow() {
        ImageEnginePresetSupport support = ImageEnginePresetSupportPolicy.forProfile(
                VisualGenerationProfile.CUSTOM_COMFY_WORKFLOW);

        assertFalse(support.builtInWorkflowAvailable());
        assertEquals("workflows/workflow-custom-comfy.json", support.workflowName());
        assertTrue(support.userMessage().contains("no ejecuta workflows personalizados automaticamente"));
    }
}

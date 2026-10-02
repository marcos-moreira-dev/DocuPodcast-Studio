package com.marcosmoreiradev.docupodcaststudio.application.visual;

import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

/** Neutral request for one chained image-to-video clip. */
public record VisualClipGenerationRequest(
        String prompt,
        String negativePrompt,
        Path startFrame,
        int width,
        int height,
        int framesPerSecond,
        double durationSeconds,
        long seed,
        VisualReferenceBundle references,
        VisualClipGenerationProfile profile,
        VisualClipWorkflowStrategy workflowStrategy,
        VisualComputeBinding computeBinding,
        VisualOutputTarget outputTarget,
        Map<String, String> consumerMetadata
) {
    public VisualClipGenerationRequest {
        prompt = optional(prompt);
        negativePrompt = optional(negativePrompt);
        startFrame = Objects.requireNonNull(startFrame, "startFrame").toAbsolutePath().normalize();
        width = Math.max(360, width);
        height = Math.max(360, height);
        framesPerSecond = Math.max(12, Math.min(60, framesPerSecond));
        durationSeconds = Math.max(0.25, Math.min(10.0, durationSeconds));
        references = Objects.requireNonNullElseGet(references, VisualReferenceBundle::empty);
        profile = Objects.requireNonNullElse(
                profile, VisualClipGenerationProfile.WAN22_TI2V_5B_BALANCED);
        if (workflowStrategy == null) {
            workflowStrategy = switch (profile) {
                case WAN22_TI2V_5B_BALANCED -> VisualClipWorkflowStrategy.WAN22_TI2V;
                case WAN22_I2V_14B_QUALITY -> VisualClipWorkflowStrategy.WAN22_I2V;
                case LTX23_I2V_PORTRAIT -> VisualClipWorkflowStrategy.LTX23_I2V;
                case CUSTOM_COMFY_VIDEO -> VisualClipWorkflowStrategy.CUSTOM_COMFY_VIDEO;
            };
        }
        computeBinding = Objects.requireNonNull(computeBinding, "computeBinding");
        outputTarget = Objects.requireNonNull(outputTarget, "outputTarget");
        consumerMetadata = consumerMetadata == null ? Map.of() : Map.copyOf(consumerMetadata);
    }

    public int frameCount() {
        return Math.max(1, (int) Math.ceil(durationSeconds * framesPerSecond));
    }

    private static String optional(String value) {
        return value == null ? "" : value.strip();
    }
}

package com.marcosmoreiradev.docupodcaststudio.application.image;

import java.nio.file.Path;
import java.util.Objects;

/** Immutable request for a local image enhancement provider. */
public record ImageEnhancementRequest(
        String jobId,
        Path sourceImage,
        Path projectRoot,
        ImageEnhancementOutputProfile outputProfile,
        ImageAspectStrategy aspectStrategy,
        ImageEnhancementPipelineProfile pipelineProfile,
        String providerId,
        String workflowId,
        String prompt,
        int steps,
        double denoise,
        int tileSize,
        int tileOverlap,
        boolean useLora,
        String loraName) {
    public ImageEnhancementRequest {
        Objects.requireNonNull(sourceImage, "sourceImage");
        Objects.requireNonNull(projectRoot, "projectRoot");
        Objects.requireNonNull(outputProfile, "outputProfile");
        aspectStrategy = aspectStrategy == null ? ImageAspectStrategy.OUTPAINT_TO_TARGET : aspectStrategy;
        pipelineProfile = pipelineProfile == null ? outputProfile.pipelineProfile() : pipelineProfile;
        jobId = jobId == null || jobId.isBlank() ? "image-enhancement-" + System.currentTimeMillis() : jobId.strip();
        providerId = providerId == null || providerId.isBlank() ? "comfyui-local" : providerId.strip();
        workflowId = workflowId == null || workflowId.isBlank() ? outputProfile.workflowId() : workflowId.strip();
        prompt = prompt == null ? "" : prompt.strip();
        steps = Math.max(1, steps);
        denoise = Math.max(0.0, Math.min(1.0, denoise));
        tileSize = tileSize <= 0 ? outputProfile.tileSize() : tileSize;
        tileOverlap = tileOverlap < 0 ? outputProfile.tileOverlap() : tileOverlap;
        loraName = loraName == null ? "" : loraName.strip();
    }
}

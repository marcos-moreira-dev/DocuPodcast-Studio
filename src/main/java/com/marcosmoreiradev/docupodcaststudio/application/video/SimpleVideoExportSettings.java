package com.marcosmoreiradev.docupodcaststudio.application.video;

import com.marcosmoreiradev.docupodcaststudio.domain.video.SimpleVideoResolutionPreset;

import com.marcosmoreiradev.docupodcaststudio.application.compute.ComputeDevicePolicy;
import com.marcosmoreiradev.docupodcaststudio.application.compute.VideoEncoderPolicy;

/** User-facing video export settings with 2K as the default resolution. */
public record SimpleVideoExportSettings(
        SimpleVideoResolutionPreset resolution,
        int framesPerSecond,
        double silenceAfterFrameSeconds,
        boolean preferEmbeddedFfmpeg,
        boolean blockMainWorkspaceDuringRender,
        ComputeDevicePolicy computePolicy,
        String selectedDeviceId,
        VideoEncoderPolicy encoderPolicy,
        boolean renderUnassignedVisuals,
        boolean includeInferredFrames
) {
    public SimpleVideoExportSettings {
        resolution = resolution == null ? SimpleVideoResolutionPreset.defaultPreset() : resolution;
        framesPerSecond = Math.max(1, framesPerSecond);
        silenceAfterFrameSeconds = Math.max(0.0, silenceAfterFrameSeconds);
        computePolicy = computePolicy == null ? ComputeDevicePolicy.AUTO : computePolicy;
        selectedDeviceId = selectedDeviceId == null ? "" : selectedDeviceId.strip();
        encoderPolicy = encoderPolicy == null ? VideoEncoderPolicy.AUTO : encoderPolicy;
    }

    /** Compatibility constructor used before selected device identity reached video. */
    public SimpleVideoExportSettings(
            SimpleVideoResolutionPreset resolution,
            int framesPerSecond,
            double silenceAfterFrameSeconds,
            boolean preferEmbeddedFfmpeg,
            boolean blockMainWorkspaceDuringRender,
            ComputeDevicePolicy computePolicy,
            VideoEncoderPolicy encoderPolicy,
            boolean renderUnassignedVisuals,
            boolean includeInferredFrames) {
        this(resolution, framesPerSecond, silenceAfterFrameSeconds,
                preferEmbeddedFfmpeg, blockMainWorkspaceDuringRender,
                computePolicy, "", encoderPolicy, renderUnassignedVisuals,
                includeInferredFrames);
    }

    public SimpleVideoExportSettings(
            SimpleVideoResolutionPreset resolution,
            int framesPerSecond,
            double silenceAfterFrameSeconds,
            boolean preferEmbeddedFfmpeg,
            boolean blockMainWorkspaceDuringRender,
            ComputeDevicePolicy computePolicy,
            VideoEncoderPolicy encoderPolicy
    ) {
        this(resolution, framesPerSecond, silenceAfterFrameSeconds, preferEmbeddedFfmpeg,
                blockMainWorkspaceDuringRender, computePolicy, "", encoderPolicy, false, false);
    }

    public SimpleVideoExportSettings(
            SimpleVideoResolutionPreset resolution,
            int framesPerSecond,
            double silenceAfterFrameSeconds,
            boolean preferEmbeddedFfmpeg,
            boolean blockMainWorkspaceDuringRender,
            ComputeDevicePolicy computePolicy,
            String selectedDeviceId,
            VideoEncoderPolicy encoderPolicy) {
        this(resolution, framesPerSecond, silenceAfterFrameSeconds,
                preferEmbeddedFfmpeg, blockMainWorkspaceDuringRender,
                computePolicy, selectedDeviceId, encoderPolicy, false, false);
    }

    public SimpleVideoExportSettings(
            SimpleVideoResolutionPreset resolution,
            int framesPerSecond,
            double silenceAfterFrameSeconds,
            boolean preferEmbeddedFfmpeg,
            boolean blockMainWorkspaceDuringRender,
            ComputeDevicePolicy computePolicy,
            VideoEncoderPolicy encoderPolicy,
            boolean renderUnassignedVisuals
    ) {
        this(resolution, framesPerSecond, silenceAfterFrameSeconds, preferEmbeddedFfmpeg,
                blockMainWorkspaceDuringRender, computePolicy, "", encoderPolicy, renderUnassignedVisuals, false);
    }

    public SimpleVideoExportSettings(
            SimpleVideoResolutionPreset resolution,
            int framesPerSecond,
            double silenceAfterFrameSeconds,
            boolean preferEmbeddedFfmpeg,
            boolean blockMainWorkspaceDuringRender
    ) {
        this(resolution, framesPerSecond, silenceAfterFrameSeconds, preferEmbeddedFfmpeg,
                blockMainWorkspaceDuringRender, ComputeDevicePolicy.AUTO, VideoEncoderPolicy.AUTO, false, false);
    }

    public static SimpleVideoExportSettings defaults() {
        return new SimpleVideoExportSettings(
                SimpleVideoResolutionPreset.defaultPreset(),
                30,
                BuildSimpleVideoPlanUseCase.DEFAULT_SILENCE_AFTER_FRAME_SECONDS,
                true,
                true,
                ComputeDevicePolicy.AUTO,
                "",
                VideoEncoderPolicy.AUTO,
                false,
                false
        );
    }

    public SimpleVideoExportSettings withRenderUnassignedVisuals(boolean enabled) {
        return new SimpleVideoExportSettings(resolution, framesPerSecond, silenceAfterFrameSeconds,
                preferEmbeddedFfmpeg, blockMainWorkspaceDuringRender, computePolicy,
                selectedDeviceId, encoderPolicy, enabled, includeInferredFrames);
    }

    public SimpleVideoExportSettings withIncludeInferredFrames(boolean enabled) {
        return new SimpleVideoExportSettings(resolution, framesPerSecond, silenceAfterFrameSeconds,
                preferEmbeddedFfmpeg, blockMainWorkspaceDuringRender, computePolicy,
                selectedDeviceId, encoderPolicy,
                renderUnassignedVisuals, enabled);
    }

    public String resolutionLabel() {
        return resolution.label() + " — " + resolution.width() + "x" + resolution.height();
    }
}

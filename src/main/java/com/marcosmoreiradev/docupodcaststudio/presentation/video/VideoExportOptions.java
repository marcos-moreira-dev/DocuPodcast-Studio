package com.marcosmoreiradev.docupodcaststudio.presentation.video;

import com.marcosmoreiradev.docupodcaststudio.application.compute.VideoEncoderPolicy;
import com.marcosmoreiradev.docupodcaststudio.domain.video.SimpleVideoResolutionPreset;

/** User-selected properties for a final MP4 export. */
public record VideoExportOptions(
        SimpleVideoResolutionPreset resolution,
        int framesPerSecond,
        VideoEncoderPolicy encoderPolicy,
        boolean renderUnassignedVisuals,
        boolean includeInferredFrames
) {
    public VideoExportOptions(SimpleVideoResolutionPreset resolution,
                              int framesPerSecond,
                              VideoEncoderPolicy encoderPolicy) {
        this(resolution, framesPerSecond, encoderPolicy, false, false);
    }

    public VideoExportOptions(SimpleVideoResolutionPreset resolution,
                              int framesPerSecond,
                              VideoEncoderPolicy encoderPolicy,
                              boolean renderUnassignedVisuals) {
        this(resolution, framesPerSecond, encoderPolicy, renderUnassignedVisuals, false);
    }

    public VideoExportOptions {
        resolution = resolution == null ? SimpleVideoResolutionPreset.defaultPreset() : resolution;
        framesPerSecond = Math.max(1, framesPerSecond);
        encoderPolicy = encoderPolicy == null ? VideoEncoderPolicy.AUTO : encoderPolicy;
    }
}

package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.io.IOException;

/** Dedicated existing-image enhancement capability; it must never regenerate a scene. */
public interface ImageSuperResolutionEngine extends MediaEngine {
    ImageSuperResolutionResult upscale(ImageSuperResolutionRequest request, ExecutionContext context)
            throws IOException, InterruptedException;
}

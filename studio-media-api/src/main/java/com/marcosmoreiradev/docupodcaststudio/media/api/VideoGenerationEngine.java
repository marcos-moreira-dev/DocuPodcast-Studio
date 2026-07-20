package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.io.IOException;

/** Generative video capability, intentionally separate from deterministic rendering. */
public interface VideoGenerationEngine extends MediaEngine {
    VideoGenerationResult generate(VideoGenerationRequest request, ExecutionContext context)
            throws IOException, InterruptedException;
}

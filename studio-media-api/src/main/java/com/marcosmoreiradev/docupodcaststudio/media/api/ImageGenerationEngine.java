package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.io.IOException;

public interface ImageGenerationEngine extends MediaEngine {
    ImageGenerationResult generate(ImageGenerationRequest request, ExecutionContext context)
            throws IOException, InterruptedException;
}

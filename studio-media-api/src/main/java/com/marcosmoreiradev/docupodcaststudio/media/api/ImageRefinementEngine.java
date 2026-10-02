package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.io.IOException;

/** Refines an existing image while preserving its composition. */
public interface ImageRefinementEngine extends MediaEngine {
    ImageRefinementResult refine(ImageRefinementRequest request, ExecutionContext context)
            throws IOException, InterruptedException;
}

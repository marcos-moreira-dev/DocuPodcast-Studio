package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.io.IOException;
import java.util.Set;

/** Transversal local engine whose inputs are generic text and visual evidence. */
public interface ContentAnalysisEngine extends MediaEngine {
    Set<ContentAnalysisOperation> operations();

    ContentAnalysisResult analyze(ContentAnalysisRequest request,
                                  ExecutionContext context)
            throws IOException, InterruptedException;
}

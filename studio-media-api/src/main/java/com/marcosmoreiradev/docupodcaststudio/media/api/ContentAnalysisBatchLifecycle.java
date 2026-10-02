package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.io.IOException;

/**
 * Optional lifecycle for content engines that can keep a local model resident
 * while several independent requests are processed.
 */
public interface ContentAnalysisBatchLifecycle {
    /**
     * Identity shared by adapters that delegate to the same physical runtime.
     */
    default Object contentAnalysisBatchIdentity() {
        return this;
    }

    default void beginContentAnalysisBatch(ExecutionContext context)
            throws IOException, InterruptedException {
    }

    default void endContentAnalysisBatch(ExecutionContext context)
            throws IOException, InterruptedException {
    }
}

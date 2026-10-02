package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.application.media.MediaCapabilityService;
import com.marcosmoreiradev.docupodcaststudio.media.api.CancellationToken;
import com.marcosmoreiradev.docupodcaststudio.media.api.ExecutionContext;
import com.marcosmoreiradev.docupodcaststudio.media.api.ProgressSink;

import java.io.IOException;
import java.util.Objects;

/**
 * Application boundary for a document-analysis batch. It deliberately exposes
 * no PDF or engine-specific runtime details.
 */
public final class RunDocumentLocalAnalysisBatchUseCase {
    private final MediaCapabilityService media;

    public RunDocumentLocalAnalysisBatchUseCase(MediaCapabilityService media) {
        this.media = Objects.requireNonNull(media, "media capability service");
    }

    public <T> T execute(String operationId, CancellationToken cancellation,
                         ProgressSink progress, BatchWork<T> work)
            throws IOException, InterruptedException {
        Objects.requireNonNull(work, "work");
        ExecutionContext context = ExecutionContext.defaults(
                operationId == null || operationId.isBlank()
                        ? "document-local-analysis-batch" : operationId);
        context = new ExecutionContext(
                context.operationId(),
                cancellation == null ? CancellationToken.NONE : cancellation,
                progress == null ? ProgressSink.NONE : progress,
                context.policy(), context.resourceLease(), context.staging(),
                context.computePreference());
        return media.withContentAnalysisBatch(context, work::run);
    }

    @FunctionalInterface
    public interface BatchWork<T> {
        T run() throws IOException, InterruptedException;
    }
}

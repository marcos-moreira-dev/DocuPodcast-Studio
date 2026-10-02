package com.marcosmoreiradev.docupodcaststudio.application.observability;

import java.time.Duration;

/** Local-only metric sink. Job manifests remain the persistent source of truth. */
public interface JobMetricsRecorder {
    void started(OperationContext context);
    void resourceWait(OperationContext context, Duration duration);
    void retry(OperationContext context, int attempt);
    void completed(OperationContext context, Duration duration, long artifactBytes);
    void failed(OperationContext context, Duration duration, String diagnostic);
    void cancelled(OperationContext context, Duration duration);

    static JobMetricsRecorder noop() {
        return NoopHolder.INSTANCE;
    }

    final class NoopHolder {
        private static final JobMetricsRecorder INSTANCE = new JobMetricsRecorder() {
            @Override public void started(OperationContext context) { }
            @Override public void resourceWait(OperationContext context, Duration duration) { }
            @Override public void retry(OperationContext context, int attempt) { }
            @Override public void completed(OperationContext context, Duration duration, long artifactBytes) { }
            @Override public void failed(OperationContext context, Duration duration, String diagnostic) { }
            @Override public void cancelled(OperationContext context, Duration duration) { }
        };
        private NoopHolder() { }
    }
}

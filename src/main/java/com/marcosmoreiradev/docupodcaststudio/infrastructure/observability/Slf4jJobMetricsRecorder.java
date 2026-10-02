package com.marcosmoreiradev.docupodcaststudio.infrastructure.observability;

import com.marcosmoreiradev.docupodcaststudio.application.observability.JobMetricsRecorder;
import com.marcosmoreiradev.docupodcaststudio.application.observability.OperationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/** Emits structured local events without persisting a second job schema. */
public final class Slf4jJobMetricsRecorder implements JobMetricsRecorder {
    private static final Logger LOGGER = LoggerFactory.getLogger("docupodcast.job.metrics");

    @Override public void started(OperationContext context) { event(context, "started", "durationMs=0"); }
    @Override public void resourceWait(OperationContext context, Duration duration) {
        event(context, "resource-wait", "durationMs=" + millis(duration));
    }
    @Override public void retry(OperationContext context, int attempt) {
        event(context, "retry", "attempt=" + Math.max(1, attempt));
    }
    @Override public void completed(OperationContext context, Duration duration, long artifactBytes) {
        event(context, "completed", "durationMs=" + millis(duration) + " artifactBytes=" + Math.max(0, artifactBytes));
    }
    @Override public void failed(OperationContext context, Duration duration, String diagnostic) {
        DiagnosticSanitizer sanitizer = new DiagnosticSanitizer(null);
        event(context, "failed", "durationMs=" + millis(duration) + " diagnostic=" + sanitizer.sanitize(diagnostic));
    }
    @Override public void cancelled(OperationContext context, Duration duration) {
        event(context, "cancelled", "durationMs=" + millis(duration));
    }

    private static void event(OperationContext context, String outcome, String fields) {
        try (OperationMdcScope ignored = OperationMdcScope.open(context)) {
            LOGGER.info("job.metric outcome={} {}", outcome, fields);
        }
    }

    private static long millis(Duration duration) {
        return duration == null ? 0 : Math.max(0, duration.toMillis());
    }
}

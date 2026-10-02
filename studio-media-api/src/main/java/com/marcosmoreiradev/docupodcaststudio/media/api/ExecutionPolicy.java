package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.time.Duration;

public record ExecutionPolicy(Duration timeout, int maxAttempts) {
    private static final Duration UNBOUNDED_TIMEOUT = Duration.ofNanos(Long.MAX_VALUE);

    public ExecutionPolicy {
        timeout = timeout == null || timeout.isNegative() || timeout.isZero() ? Duration.ofMinutes(5) : timeout;
        maxAttempts = Math.max(1, Math.min(10, maxAttempts));
    }

    public static ExecutionPolicy defaults() { return new ExecutionPolicy(Duration.ofMinutes(5), 1); }

    /** Cooperative cancellation remains active, but elapsed time never aborts the operation. */
    public static ExecutionPolicy unbounded() {
        return new ExecutionPolicy(UNBOUNDED_TIMEOUT, 1);
    }

    public boolean hasTimeout() {
        return !UNBOUNDED_TIMEOUT.equals(timeout);
    }
}

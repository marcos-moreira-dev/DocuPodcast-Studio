package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.time.Duration;

public record ExecutionPolicy(Duration timeout, int maxAttempts) {
    public ExecutionPolicy {
        timeout = timeout == null || timeout.isNegative() || timeout.isZero() ? Duration.ofMinutes(5) : timeout;
        maxAttempts = Math.max(1, Math.min(10, maxAttempts));
    }

    public static ExecutionPolicy defaults() { return new ExecutionPolicy(Duration.ofMinutes(5), 1); }
}

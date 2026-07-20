package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.util.Objects;

public record ExecutionContext(
        String operationId,
        CancellationToken cancellation,
        ProgressSink progress,
        ExecutionPolicy policy,
        ResourceLease resourceLease) {
    public ExecutionContext {
        operationId = operationId == null || operationId.isBlank() ? "media-operation" : operationId.strip();
        cancellation = Objects.requireNonNullElse(cancellation, CancellationToken.NONE);
        progress = Objects.requireNonNullElse(progress, ProgressSink.NONE);
        policy = Objects.requireNonNullElse(policy, ExecutionPolicy.defaults());
        resourceLease = Objects.requireNonNullElse(resourceLease, ResourceLease.NONE);
    }

    public static ExecutionContext defaults(String operationId) {
        return new ExecutionContext(operationId, CancellationToken.NONE, ProgressSink.NONE,
                ExecutionPolicy.defaults(), ResourceLease.NONE);
    }
}

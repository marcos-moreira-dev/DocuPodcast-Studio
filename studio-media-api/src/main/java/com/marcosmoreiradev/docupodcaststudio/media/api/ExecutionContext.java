package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.util.Objects;

public record ExecutionContext(
        String operationId,
        CancellationToken cancellation,
        ProgressSink progress,
        ExecutionPolicy policy,
        ResourceLease resourceLease,
        GenerationArtifactStaging staging,
        ComputePreference computePreference,
        OperationDeadline deadline) {
    public ExecutionContext {
        operationId = operationId == null || operationId.isBlank() ? "media-operation" : operationId.strip();
        cancellation = Objects.requireNonNullElse(cancellation, CancellationToken.NONE);
        progress = Objects.requireNonNullElse(progress, ProgressSink.NONE);
        policy = Objects.requireNonNullElse(policy, ExecutionPolicy.defaults());
        resourceLease = Objects.requireNonNullElse(resourceLease, ResourceLease.NONE);
        staging = Objects.requireNonNullElse(staging, GenerationArtifactStaging.NONE);
        computePreference = Objects.requireNonNullElse(computePreference, ComputePreference.automatic());
        deadline = Objects.requireNonNullElse(deadline, OperationDeadline.none());
    }

    public ExecutionContext(String operationId, CancellationToken cancellation,
                            ProgressSink progress, ExecutionPolicy policy,
                            ResourceLease resourceLease,
                            GenerationArtifactStaging staging,
                            ComputePreference computePreference) {
        this(operationId, cancellation, progress, policy, resourceLease, staging,
                computePreference, OperationDeadline.none());
    }

    public ExecutionContext(String operationId, CancellationToken cancellation, ProgressSink progress,
                            ExecutionPolicy policy, ResourceLease resourceLease,
                            GenerationArtifactStaging staging) {
        this(operationId, cancellation, progress, policy, resourceLease, staging,
                ComputePreference.automatic(), OperationDeadline.none());
    }

    public ExecutionContext(String operationId, CancellationToken cancellation, ProgressSink progress,
                            ExecutionPolicy policy, ResourceLease resourceLease) {
        this(operationId, cancellation, progress, policy, resourceLease,
                GenerationArtifactStaging.NONE, ComputePreference.automatic(),
                OperationDeadline.none());
    }

    public static ExecutionContext defaults(String operationId) {
        return new ExecutionContext(operationId, CancellationToken.NONE, ProgressSink.NONE,
                ExecutionPolicy.defaults(), ResourceLease.NONE, GenerationArtifactStaging.NONE,
                ComputePreference.automatic(), OperationDeadline.none());
    }

    public ExecutionContext withDeadline(OperationDeadline value) {
        return new ExecutionContext(operationId, cancellation, progress, policy,
                resourceLease, staging, computePreference, value);
    }
}

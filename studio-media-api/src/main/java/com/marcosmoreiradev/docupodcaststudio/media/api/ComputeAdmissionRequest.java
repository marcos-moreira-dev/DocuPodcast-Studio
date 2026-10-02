package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.util.Objects;
import java.util.UUID;

/** Immutable request submitted to the shared compute scheduler. */
public record ComputeAdmissionRequest(
        String admissionId,
        String operationId,
        ComputeJobPriority priority,
        ComputeWorkloadKind workload,
        ComputeResourceDemand demand,
        CancellationToken cancellation,
        OperationDeadline deadline
) {
    public ComputeAdmissionRequest {
        admissionId = normalized(admissionId,
                "compute-" + UUID.randomUUID());
        operationId = normalized(operationId, admissionId);
        priority = Objects.requireNonNullElse(priority, ComputeJobPriority.BACKGROUND);
        workload = Objects.requireNonNullElse(workload, ComputeWorkloadKind.OTHER);
        demand = Objects.requireNonNullElse(demand, ComputeResourceDemand.NONE);
        cancellation = Objects.requireNonNullElse(cancellation, CancellationToken.NONE);
        deadline = Objects.requireNonNullElse(deadline, OperationDeadline.none());
    }

    public ComputeAdmissionRequest(String admissionId, String operationId,
                                   ComputeJobPriority priority,
                                   ComputeWorkloadKind workload,
                                   ComputeResourceDemand demand,
                                   CancellationToken cancellation) {
        this(admissionId, operationId, priority, workload, demand, cancellation,
                OperationDeadline.none());
    }

    public static ComputeAdmissionRequest legacy(ResourceRequirement requirement,
                                                  CancellationToken cancellation) {
        return new ComputeAdmissionRequest("", "legacy-media-operation",
                ComputeJobPriority.BACKGROUND, ComputeWorkloadKind.OTHER,
                ComputeResourceDemand.from(requirement), cancellation,
                OperationDeadline.none());
    }

    private static String normalized(String value, String fallback) {
        String normalized = Objects.toString(value, "").strip();
        return normalized.isBlank() ? fallback : normalized;
    }
}

package com.marcosmoreiradev.docupodcaststudio.media.api;

/** Acquires scarce resources inside worker threads, never from presentation. */
public interface ResourceScheduler {
    ResourceLease acquire(ComputeAdmissionRequest request) throws InterruptedException;

    default ResourceLease acquire(ResourceRequirement requirement,
                                  CancellationToken cancellation)
            throws InterruptedException {
        return acquire(ComputeAdmissionRequest.legacy(requirement, cancellation));
    }

    default ComputeQueueSnapshot snapshot() {
        return new ComputeQueueSnapshot(
                java.util.Map.of(), java.util.Map.of(),
                java.util.List.of(), java.util.List.of());
    }

    /** Cancels queued work or requests a safe-boundary stop for active work. */
    default boolean cancel(String admissionId) {
        return false;
    }
}

package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.util.Map;

/**
 * Compatibility name for the global priority scheduler.
 *
 * @deprecated use {@link PriorityResourceScheduler}; retained while callers are
 * migrated without duplicating scheduling state.
 */
@Deprecated(forRemoval = false)
public final class LocalResourceScheduler implements ResourceScheduler {
    private final PriorityResourceScheduler delegate;

    public LocalResourceScheduler(Map<ResourceId, Integer> capacities) {
        this.delegate = new PriorityResourceScheduler(capacities);
    }

    public static LocalResourceScheduler safeDefaults() {
        return new LocalResourceScheduler(
                ComputeResourceBudget.safeDefaults().legacyCapacities());
    }

    @Override
    public ResourceLease acquire(ComputeAdmissionRequest request)
            throws InterruptedException {
        return delegate.acquire(request);
    }

    @Override
    public ComputeQueueSnapshot snapshot() {
        return delegate.snapshot();
    }

    @Override
    public boolean cancel(String admissionId) {
        return delegate.cancel(admissionId);
    }
}

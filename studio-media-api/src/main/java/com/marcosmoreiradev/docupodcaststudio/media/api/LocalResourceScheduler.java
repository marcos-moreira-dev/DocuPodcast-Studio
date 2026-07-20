package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;

/** Deadlock-safe local scheduler with deterministic acquisition order. */
public final class LocalResourceScheduler implements ResourceScheduler {
    private final Map<ResourceId, Semaphore> resources;

    public LocalResourceScheduler(Map<ResourceId, Integer> capacities) {
        LinkedHashMap<ResourceId, Semaphore> configured = new LinkedHashMap<>();
        if (capacities != null) {
            capacities.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> configured.put(entry.getKey(),
                            new Semaphore(Math.max(1, entry.getValue()), true)));
        }
        this.resources = new ConcurrentHashMap<>(configured);
    }

    public static LocalResourceScheduler safeDefaults() {
        return new LocalResourceScheduler(Map.of(
                ResourceId.MODEL_MEMORY, 1,
                ResourceId.GPU, 1,
                ResourceId.CPU_HEAVY, 1,
                ResourceId.VIDEO_ENCODER, 1));
    }

    @Override
    public ResourceLease acquire(ResourceRequirement requirement, CancellationToken cancellation)
            throws InterruptedException {
        ResourceRequirement requested = requirement == null ? ResourceRequirement.NONE : requirement;
        CancellationToken token = cancellation == null ? CancellationToken.NONE : cancellation;
        List<Map.Entry<ResourceId, Semaphore>> ordered = requested.resources().stream()
                .sorted(Comparator.naturalOrder())
                .map(id -> Map.entry(id, resources.computeIfAbsent(id, ignored -> new Semaphore(1, true))))
                .toList();
        ArrayList<Semaphore> acquired = new ArrayList<>();
        try {
            for (Map.Entry<ResourceId, Semaphore> entry : ordered) {
                while (!entry.getValue().tryAcquire(100, TimeUnit.MILLISECONDS)) {
                    token.throwIfCancellationRequested();
                }
                acquired.add(entry.getValue());
            }
        } catch (InterruptedException failure) {
            acquired.forEach(Semaphore::release);
            throw failure;
        }
        return new ResourceLease() {
            private boolean closed;
            @Override public synchronized void close() {
                if (closed) return;
                closed = true;
                for (int index = acquired.size() - 1; index >= 0; index--) acquired.get(index).release();
            }
        };
    }
}

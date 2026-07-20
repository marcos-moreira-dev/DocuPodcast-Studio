package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Explicit fake/diagnostic repository; production launchers should inject durable persistence. */
public final class InMemoryGenerationJobRepository implements GenerationJobRepository {
    private final Map<GenerationJobId, GenerationJobSnapshot> snapshots = new LinkedHashMap<>();

    @Override public synchronized void save(GenerationJobSnapshot snapshot) {
        snapshots.put(snapshot.request().jobId(), snapshot);
    }
    @Override public synchronized Optional<GenerationJobSnapshot> find(GenerationJobId id) {
        return Optional.ofNullable(snapshots.get(id));
    }
    @Override public synchronized List<GenerationJobSnapshot> list() { return List.copyOf(snapshots.values()); }
}

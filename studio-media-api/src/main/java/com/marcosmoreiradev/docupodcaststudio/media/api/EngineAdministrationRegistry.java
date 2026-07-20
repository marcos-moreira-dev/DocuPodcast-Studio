package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Explicit registry for declarative engine maintenance. */
public final class EngineAdministrationRegistry {
    private final Map<EngineId, EngineAdministration> entries = new LinkedHashMap<>();

    public synchronized EngineAdministrationRegistry register(EngineAdministration administration) {
        if (administration == null) throw new IllegalArgumentException("engine administration is required");
        if (entries.putIfAbsent(administration.engineId(), administration) != null) {
            throw new IllegalArgumentException("duplicate engine administration: " + administration.engineId());
        }
        return this;
    }

    public synchronized Optional<EngineAdministration> find(EngineId id) {
        return Optional.ofNullable(entries.get(id));
    }

    public synchronized EngineAdministration require(EngineId id) {
        return find(id).orElseThrow(() -> new IllegalArgumentException("engine administration not registered: " + id));
    }

    public synchronized List<EngineAdministration> entries() { return List.copyOf(entries.values()); }
}

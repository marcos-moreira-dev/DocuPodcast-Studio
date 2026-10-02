package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Explicit compile-time registry. Duplicate ids fail during composition. */
public final class EngineRegistry<E extends MediaEngine> {
    private final CapabilityId capability;
    private final Map<EngineId, E> engines = new LinkedHashMap<>();

    public EngineRegistry(CapabilityId capability) {
        this.capability = Objects.requireNonNull(capability, "capability");
    }

    public synchronized EngineRegistry<E> register(E engine) {
        E value = Objects.requireNonNull(engine, "engine");
        EngineDescriptor descriptor = value.descriptor();
        if (!capability.equals(descriptor.capability())) {
            throw new IllegalArgumentException("engine " + descriptor.id() + " does not provide " + capability);
        }
        if (engines.putIfAbsent(descriptor.id(), value) != null) {
            throw new IllegalArgumentException("duplicate engine id: " + descriptor.id());
        }
        return this;
    }

    public synchronized Optional<E> find(EngineId id) { return Optional.ofNullable(engines.get(id)); }

    public synchronized E require(EngineId id) {
        return find(id).orElseThrow(() -> new IllegalArgumentException("engine not registered: " + id));
    }

    public synchronized List<E> engines() { return List.copyOf(new ArrayList<>(engines.values())); }

    public synchronized List<EngineDescriptor> descriptors() {
        return engines.values().stream().map(MediaEngine::descriptor).toList();
    }

    public CapabilityId capability() { return capability; }
}

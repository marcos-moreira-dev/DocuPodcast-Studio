package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.util.Map;
import java.util.Objects;

/** Namespaced persisted values; each adapter maps them into its typed private configuration. */
public record EngineConfiguration(EngineId engineId, Map<String, String> values) {
    public EngineConfiguration {
        Objects.requireNonNull(engineId, "engineId");
        values = values == null ? Map.of() : Map.copyOf(values);
    }

    public String value(String key) { return values.getOrDefault(key, ""); }
}

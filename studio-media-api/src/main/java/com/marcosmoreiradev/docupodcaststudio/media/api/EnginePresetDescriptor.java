package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.util.Map;
import java.util.Set;

/** Declarative generation preset supplied by an engine adapter. */
public record EnginePresetDescriptor(
        EnginePresetId id,
        String displayName,
        String description,
        Set<EngineFeature> features,
        Map<String, String> metadata,
        boolean defaultPreset) {
    public EnginePresetDescriptor {
        if (id == null) throw new IllegalArgumentException("preset id is required");
        displayName = displayName == null || displayName.isBlank() ? id.value() : displayName.strip();
        description = description == null ? "" : description.strip();
        features = features == null ? Set.of() : Set.copyOf(features);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}

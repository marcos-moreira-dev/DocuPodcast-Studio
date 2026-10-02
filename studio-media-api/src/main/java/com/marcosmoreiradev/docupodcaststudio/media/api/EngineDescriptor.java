package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.util.Objects;
import java.util.Set;

/** Provider-neutral metadata used by selection, settings and presentation. */
public record EngineDescriptor(
        EngineId id,
        CapabilityId capability,
        String displayName,
        String version,
        String runtimeKind,
        Set<EngineFeature> features,
        boolean diagnosticOnly) {
    public EngineDescriptor {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(capability, "capability");
        displayName = clean(displayName, id.value());
        version = clean(version, "unknown");
        runtimeKind = clean(runtimeKind, "local");
        features = features == null ? Set.of() : Set.copyOf(features);
    }

    public boolean supports(EngineFeature feature) {
        return feature != null && features.contains(feature);
    }

    private static String clean(String value, String fallback) {
        String text = value == null ? "" : value.strip();
        return text.isBlank() ? fallback : text;
    }
}

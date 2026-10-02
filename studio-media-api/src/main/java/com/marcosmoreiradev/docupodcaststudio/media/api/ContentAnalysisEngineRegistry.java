package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** One analysis registry shared by all product modules. */
public final class ContentAnalysisEngineRegistry {
    private final Map<EngineId, ContentAnalysisEngine> engines =
            new LinkedHashMap<>();

    public synchronized ContentAnalysisEngineRegistry register(
            ContentAnalysisEngine engine) {
        ContentAnalysisEngine value = Objects.requireNonNull(engine, "engine");
        if (!isAnalysisCapability(value.descriptor().capability())) {
            throw new IllegalArgumentException("unsupported analysis capability: "
                    + value.descriptor().capability().value());
        }
        if (engines.putIfAbsent(value.descriptor().id(), value) != null) {
            throw new IllegalArgumentException(
                    "duplicate analysis engine id: " + value.descriptor().id());
        }
        return this;
    }

    public synchronized Optional<ContentAnalysisEngine> find(EngineId id) {
        return Optional.ofNullable(engines.get(id));
    }

    public synchronized List<ContentAnalysisEngine> engines() {
        return List.copyOf(engines.values());
    }

    public synchronized List<ContentAnalysisEngine> supporting(
            ContentAnalysisOperation operation) {
        return engines.values().stream()
                .filter(engine -> engine.operations().contains(operation)).toList();
    }

    private static boolean isAnalysisCapability(CapabilityId capability) {
        return CapabilityId.CONTENT_LAYOUT_ANALYSIS.equals(capability)
                || CapabilityId.CONTENT_MATH_RECOGNITION.equals(capability)
                || CapabilityId.MATH_SPEECH.equals(capability)
                || CapabilityId.VISUAL_CONTENT_DESCRIPTION.equals(capability)
                || CapabilityId.CONTENT_CONTEXT_CORRECTION.equals(capability)
                || CapabilityId.CONTENT_TABLE_ANALYSIS.equals(capability)
                || CapabilityId.CONTENT_NARRATABILITY_ANALYSIS.equals(capability)
                || CapabilityId.NARRATION_TRANSLATION.equals(capability);
    }
}

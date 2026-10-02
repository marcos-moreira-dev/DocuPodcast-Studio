package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.util.List;
import java.util.Map;

/** Presentation-ready metadata for one adapter maintenance operation. */
public record EngineActionDescriptor(
        EngineActionId id,
        String displayName,
        String description,
        List<EngineConfigurationField> inputs,
        boolean confirmationRequired,
        long approximateBytes,
        Map<String, String> metadata) {
    public EngineActionDescriptor {
        if (id == null) throw new IllegalArgumentException("action id is required");
        displayName = displayName == null || displayName.isBlank() ? id.value() : displayName.strip();
        description = description == null ? "" : description.strip();
        inputs = inputs == null ? List.of() : List.copyOf(inputs);
        approximateBytes = Math.max(0L, approximateBytes);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}

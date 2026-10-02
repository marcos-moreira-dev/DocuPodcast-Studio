package com.marcosmoreiradev.docupodcaststudio.application.media.administration;

import com.marcosmoreiradev.docupodcaststudio.media.api.CapabilityId;
import com.marcosmoreiradev.docupodcaststudio.media.api.EngineActionDescriptor;
import com.marcosmoreiradev.docupodcaststudio.media.api.EngineId;

import java.util.List;
import java.util.Map;

/** Neutral component metadata rendered exclusively by the settings module. */
public record ManagedComponentDescriptor(
        String componentId,
        String displayName,
        String category,
        CapabilityId capability,
        EngineId engineId,
        List<String> dependencies,
        long approximateBytes,
        String license,
        String source,
        List<EngineActionDescriptor> actions,
        Map<String, String> metadata) {
    public ManagedComponentDescriptor {
        componentId = componentId == null ? "" : componentId.strip();
        displayName = displayName == null || displayName.isBlank() ? componentId : displayName.strip();
        category = category == null ? "" : category.strip();
        dependencies = dependencies == null ? List.of() : List.copyOf(dependencies);
        license = license == null ? "" : license.strip();
        source = source == null ? "" : source.strip();
        actions = actions == null ? List.of() : List.copyOf(actions);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        approximateBytes = Math.max(0L, approximateBytes);
    }
}

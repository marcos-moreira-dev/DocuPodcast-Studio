package com.marcosmoreiradev.docupodcaststudio.application.media.administration;

import com.marcosmoreiradev.docupodcaststudio.media.api.CapabilityId;
import com.marcosmoreiradev.docupodcaststudio.media.api.EngineActionId;
import com.marcosmoreiradev.docupodcaststudio.media.api.EngineId;
import com.marcosmoreiradev.docupodcaststudio.media.api.EnginePresetId;

import java.util.Map;

/** One requested operation with enough context to verify the exact preset. */
public record CapabilityRequirement(
        CapabilityId capability,
        EngineId engineId,
        EnginePresetId presetId,
        EngineActionId operation,
        Map<String, String> inputs) {
    public CapabilityRequirement {
        if (capability == null) throw new IllegalArgumentException("capability is required");
        inputs = inputs == null ? Map.of() : Map.copyOf(inputs);
    }
}

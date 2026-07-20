package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.util.Map;

public record EngineActionRequest(
        EngineId engineId,
        EngineActionId actionId,
        Map<String, String> inputs) {
    public EngineActionRequest {
        if (engineId == null) throw new IllegalArgumentException("engine id is required");
        if (actionId == null) throw new IllegalArgumentException("action id is required");
        inputs = inputs == null ? Map.of() : Map.copyOf(inputs);
    }
}

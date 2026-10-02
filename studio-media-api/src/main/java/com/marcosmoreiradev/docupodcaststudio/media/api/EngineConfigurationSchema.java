package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.util.List;
import java.util.Objects;

public record EngineConfigurationSchema(EngineId engineId, List<EngineConfigurationField> fields) {
    public EngineConfigurationSchema {
        Objects.requireNonNull(engineId, "engineId");
        fields = fields == null ? List.of() : List.copyOf(fields);
    }
}

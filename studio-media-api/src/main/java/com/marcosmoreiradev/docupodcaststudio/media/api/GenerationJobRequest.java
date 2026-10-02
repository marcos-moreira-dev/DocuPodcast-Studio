package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/** Neutral persisted request envelope; provider protocols stay in adapter-specific execution. */
public record GenerationJobRequest(
        GenerationJobId jobId,
        CapabilityId capabilityId,
        EngineId engineId,
        Map<String, String> parameters,
        Instant createdAt,
        int schemaVersion,
        EnginePresetId presetId,
        GenerationPayload payload) {
    public GenerationJobRequest {
        jobId = Objects.requireNonNull(jobId, "jobId");
        capabilityId = Objects.requireNonNull(capabilityId, "capabilityId");
        engineId = Objects.requireNonNull(engineId, "engineId");
        parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
        createdAt = Objects.requireNonNullElseGet(createdAt, Instant::now);
        schemaVersion = Math.max(1, schemaVersion);
        presetId = presetId == null ? EnginePresetId.AUTO : presetId;
        payload = payload == null ? new EmptyGenerationPayload(parameters) : payload;
    }

    public GenerationJobRequest(GenerationJobId jobId, CapabilityId capabilityId, EngineId engineId,
                                Map<String, String> parameters, Instant createdAt) {
        this(jobId, capabilityId, engineId, parameters, createdAt, 2,
                EnginePresetId.AUTO, new EmptyGenerationPayload(parameters));
    }
}

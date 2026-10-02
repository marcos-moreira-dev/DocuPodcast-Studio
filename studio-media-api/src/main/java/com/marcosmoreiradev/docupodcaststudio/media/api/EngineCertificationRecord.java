package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/** Evidence that a concrete runtime/model/hardware combination passed a real smoke test. */
public record EngineCertificationRecord(
        EngineId engineId,
        String runtimeVersion,
        String modelId,
        String hardwareFingerprint,
        String smokeTestId,
        Instant certifiedAt,
        boolean visualInputVerified,
        Map<String, String> diagnostics) {
    public EngineCertificationRecord {
        Objects.requireNonNull(engineId, "engineId");
        runtimeVersion = Objects.toString(runtimeVersion, "").strip();
        modelId = Objects.toString(modelId, "").strip();
        hardwareFingerprint = Objects.toString(hardwareFingerprint, "").strip();
        smokeTestId = Objects.toString(smokeTestId, "").strip();
        certifiedAt = Objects.requireNonNullElseGet(certifiedAt, Instant::now);
        diagnostics = diagnostics == null ? Map.of() : Map.copyOf(diagnostics);
    }

    public boolean matches(String runtime, String model, String hardware, boolean requireVisual) {
        return runtimeVersion.equals(Objects.toString(runtime, "").strip())
                && modelId.equals(Objects.toString(model, "").strip())
                && hardwareFingerprint.equals(Objects.toString(hardware, "").strip())
                && (!requireVisual || visualInputVerified);
    }
}

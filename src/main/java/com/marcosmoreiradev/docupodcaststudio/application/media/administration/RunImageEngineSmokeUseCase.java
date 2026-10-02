package com.marcosmoreiradev.docupodcaststudio.application.media.administration;

import com.marcosmoreiradev.docupodcaststudio.media.api.*;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

/** Owns readiness, lifecycle and one real image smoke operation. */
public final class RunImageEngineSmokeUseCase {
    private final CapabilityAdministrationService capabilities;

    public RunImageEngineSmokeUseCase(CapabilityAdministrationService capabilities) {
        this.capabilities = Objects.requireNonNull(capabilities, "capability administration service");
    }

    public EngineActionResult run(EngineId engineId, Map<String, String> inputs,
                                  ExecutionContext context)
            throws IOException, InterruptedException {
        CapabilityRequirement requirement = new CapabilityRequirement(
                CapabilityId.IMAGE_GENERATION, engineId,
                inputs == null ? null : new EnginePresetId(inputs.getOrDefault("presetId", "draft")),
                EngineActionId.SMOKE_TEST, inputs);
        CapabilityReadinessReport readiness = capabilities.inspect(requirement);
        boolean lifecycleAcquired = false;
        try {
            if (!readiness.ready()) {
                EngineActionResult start = capabilities.execute(
                        new EngineActionRequest(engineId, EngineActionId.START,
                                inputs == null ? Map.of() : inputs), context);
                if (!start.success()) return start;
                lifecycleAcquired = true;
                readiness = capabilities.inspect(requirement);
            }
            if (!readiness.ready()) {
                EngineReadiness unavailable = new EngineReadiness(engineId, ReadinessState.UNAVAILABLE,
                        readiness.summary(), readiness.issues(), readiness.recommendedActions(),
                        readiness.technicalDetails());
                return new EngineActionResult(false, readiness.summary(), unavailable, java.util.List.of(),
                        Map.of("state", readiness.state().name()));
            }
            return capabilities.execute(new EngineActionRequest(
                    engineId, EngineActionId.SMOKE_TEST, inputs), context);
        } finally {
            if (lifecycleAcquired) {
                try {
                    capabilities.execute(new EngineActionRequest(
                            engineId, EngineActionId.STOP, Map.of()), context);
                } catch (IOException | InterruptedException stopFailure) {
                    if (stopFailure instanceof InterruptedException) Thread.currentThread().interrupt();
                }
            }
        }
    }
}

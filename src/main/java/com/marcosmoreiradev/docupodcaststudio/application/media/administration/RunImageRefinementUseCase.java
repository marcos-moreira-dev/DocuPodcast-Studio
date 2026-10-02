package com.marcosmoreiradev.docupodcaststudio.application.media.administration;

import com.marcosmoreiradev.docupodcaststudio.media.api.*;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

/** Readiness-checked entry point for composition-preserving image refinement. */
public final class RunImageRefinementUseCase {
    private final CapabilityAdministrationService capabilities;

    public RunImageRefinementUseCase(CapabilityAdministrationService capabilities) {
        this.capabilities = Objects.requireNonNull(capabilities, "capabilities");
    }

    public ImageRefinementResult run(
            EngineId lifecycleEngineId,
            EngineId engineId,
            ImageRefinementRequest request,
            ExecutionContext context) throws IOException, InterruptedException {
        CapabilityRequirement requirement = new CapabilityRequirement(
                CapabilityId.IMAGE_REFINEMENT, engineId, request.presetId(),
                null, Map.of("operation", "refine-existing-image"));
        CapabilityReadinessReport readiness = capabilities.inspect(requirement);
        if (!readiness.ready() && readiness.state() == CapabilityReadinessState.MISSING) {
            throw new IOException(readiness.summary() + details(readiness));
        }
        boolean lifecycleAcquired = false;
        try {
            if (!readiness.ready()) {
                EngineActionResult start = capabilities.execute(new EngineActionRequest(
                        lifecycleEngineId, EngineActionId.START, Map.of()), context);
                if (!start.success()) throw new IOException(start.message());
                lifecycleAcquired = true;
                readiness = capabilities.inspect(requirement);
            }
            if (!readiness.ready()) {
                throw new IOException(readiness.summary() + details(readiness));
            }
            return capabilities.platform().imageRefinementEngines().require(engineId).refine(request, context);
        } finally {
            if (lifecycleAcquired) {
                try {
                    capabilities.execute(new EngineActionRequest(
                            lifecycleEngineId, EngineActionId.STOP, Map.of()), context);
                } catch (IOException | InterruptedException stopFailure) {
                    if (stopFailure instanceof InterruptedException) Thread.currentThread().interrupt();
                }
            }
        }
    }

    private static String details(CapabilityReadinessReport report) {
        if (report == null) return "";
        String recommendations = String.join(" ", report.recommendedActions());
        String technical = report.technicalDetails();
        if (recommendations.isBlank() && technical.isBlank()) return "";
        return " " + (recommendations + " " + technical).strip();
    }
}

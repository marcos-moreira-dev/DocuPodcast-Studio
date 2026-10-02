package com.marcosmoreiradev.docupodcaststudio.application.media.administration;

import com.marcosmoreiradev.docupodcaststudio.media.api.*;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

/** Applies superresolution to an existing image while owning the shared runtime lifecycle. */
public final class RunImageSuperResolutionUseCase {
    private final CapabilityAdministrationService capabilities;

    public RunImageSuperResolutionUseCase(CapabilityAdministrationService capabilities) {
        this.capabilities = Objects.requireNonNull(capabilities, "capability administration service");
    }

    public ImageSuperResolutionResult run(
            EngineId lifecycleEngineId,
            EngineId superResolutionEngineId,
            ImageSuperResolutionRequest request,
            ExecutionContext context) throws IOException, InterruptedException {
        CapabilityRequirement requirement = new CapabilityRequirement(
                CapabilityId.IMAGE_SUPER_RESOLUTION, superResolutionEngineId,
                null, null, Map.of());
        CapabilityReadinessReport readiness = capabilities.inspect(requirement);
        if (!readiness.ready() && readiness.state() == CapabilityReadinessState.MISSING) {
            throw new IOException(readiness.summary() + " "
                    + String.join(" ", readiness.recommendedActions()));
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
                throw new IOException(readiness.summary() + " "
                        + String.join(" ", readiness.recommendedActions()));
            }
            ImageSuperResolutionEngine engine = capabilities.platform()
                    .imageSuperResolutionEngines().require(superResolutionEngineId);
            return engine.upscale(request, context);
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
}

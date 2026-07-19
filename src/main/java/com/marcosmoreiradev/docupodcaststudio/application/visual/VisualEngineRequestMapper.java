package com.marcosmoreiradev.docupodcaststudio.application.visual;

/** Boundary mapper from the public transverse request to the ComfyUI client DTO. */
public final class VisualEngineRequestMapper {
    public VisualEngineRequest map(VisualGenerationRequest request,
                                   String checkpointName,
                                   int steps,
                                   double cfg,
                                   int batchSize,
                                   String filenamePrefix) {
        if (request == null) {
            throw new IllegalArgumentException("Falta la solicitud visual.");
        }
        return new VisualEngineRequest(
                request.prompt(),
                request.negativePrompt(),
                checkpointName,
                steps,
                cfg,
                batchSize,
                request.width(),
                request.height(),
                request.outputTarget().outputDirectory(),
                filenamePrefix,
                request.references().references(),
                seed(request));
    }

    private static long seed(VisualGenerationRequest request) {
        String configured = request.metadata().getOrDefault("seed", "");
        try {
            return Math.max(0L, Long.parseLong(configured));
        } catch (NumberFormatException ignored) {
            return java.util.concurrent.ThreadLocalRandom.current().nextLong(Long.MAX_VALUE);
        }
    }
}

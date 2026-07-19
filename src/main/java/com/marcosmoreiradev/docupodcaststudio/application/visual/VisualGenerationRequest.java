package com.marcosmoreiradev.docupodcaststudio.application.visual;

import java.util.Map;

/** Category-neutral request for one local visual generation. */
public record VisualGenerationRequest(
        String prompt,
        String negativePrompt,
        VisualReferenceBundle references,
        int width,
        int height,
        VisualGenerationProfile profile,
        VisualComputeBinding computeBinding,
        VisualOutputTarget outputTarget,
        Map<String, String> metadata
) {
    public VisualGenerationRequest {
        prompt = clean(prompt);
        negativePrompt = clean(negativePrompt);
        references = references == null ? VisualReferenceBundle.empty() : references;
        width = multipleOfEight(width <= 0 ? 1024 : width);
        height = multipleOfEight(height <= 0 ? 1024 : height);
        profile = profile == null ? VisualGenerationProfile.DIAGNOSTIC_SD15 : profile;
        if (computeBinding == null) {
            throw new IllegalArgumentException("La generacion visual requiere un dispositivo seleccionado.");
        }
        if (outputTarget == null) {
            throw new IllegalArgumentException("La generacion visual requiere un destino de salida.");
        }
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    private static String clean(String value) {
        return value == null ? "" : value.strip();
    }

    private static int multipleOfEight(int value) {
        return Math.max(8, Math.round(value / 8.0f) * 8);
    }
}

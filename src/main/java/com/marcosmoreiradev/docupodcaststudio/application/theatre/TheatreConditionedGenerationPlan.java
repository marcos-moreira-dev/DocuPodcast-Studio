package com.marcosmoreiradev.docupodcaststudio.application.theatre;

import com.marcosmoreiradev.docupodcaststudio.media.api.MediaReference;

import java.util.List;
import java.util.Map;

/** Auditable plan for generating exactly one theatre intervention with visual conditioning. */
public record TheatreConditionedGenerationPlan(
        String prompt,
        String negativePrompt,
        long seed,
        List<MediaReference> references,
        List<String> warnings,
        Map<String, String> provenance
) {
    public TheatreConditionedGenerationPlan {
        prompt = prompt == null ? "" : prompt.strip();
        negativePrompt = negativePrompt == null ? "" : negativePrompt.strip();
        references = references == null ? List.of() : List.copyOf(references);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
        provenance = provenance == null ? Map.of() : Map.copyOf(provenance);
        seed = Math.max(0L, seed);
    }
}

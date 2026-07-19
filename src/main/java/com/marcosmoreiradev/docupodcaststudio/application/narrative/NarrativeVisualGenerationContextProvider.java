package com.marcosmoreiradev.docupodcaststudio.application.narrative;

import com.marcosmoreiradev.docupodcaststudio.application.visual.VisualGenerationContextProvider;
import com.marcosmoreiradev.docupodcaststudio.application.visual.VisualGenerationRequest;

import java.util.LinkedHashMap;
import java.util.Map;

/** Narrative adapter reserved for character, location, continuity and style references. */
public final class NarrativeVisualGenerationContextProvider
        implements VisualGenerationContextProvider<NarrativeVisualGenerationInput> {
    @Override
    public VisualGenerationRequest build(NarrativeVisualGenerationInput source) {
        if (source == null) {
            throw new IllegalArgumentException("Falta el contexto narrativo para generar la imagen.");
        }
        LinkedHashMap<String, String> metadata = new LinkedHashMap<>();
        metadata.put("consumer", "narrative-video");
        if (source.metadata() != null) {
            metadata.putAll(source.metadata());
        }
        return new VisualGenerationRequest(
                source.prompt(), source.negativePrompt(), source.references(),
                source.width(), source.height(), source.profile(), source.computeBinding(),
                source.outputTarget(), Map.copyOf(metadata));
    }
}

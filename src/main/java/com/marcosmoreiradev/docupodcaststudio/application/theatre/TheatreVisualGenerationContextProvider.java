package com.marcosmoreiradev.docupodcaststudio.application.theatre;

import com.marcosmoreiradev.docupodcaststudio.application.visual.VisualGenerationContextProvider;
import com.marcosmoreiradev.docupodcaststudio.application.visual.VisualGenerationRequest;
import com.marcosmoreiradev.docupodcaststudio.application.visual.VisualReferenceBundle;

import java.util.LinkedHashMap;
import java.util.Map;

/** Adapts theatre concepts without leaking them into the shared visual engine. */
public final class TheatreVisualGenerationContextProvider
        implements VisualGenerationContextProvider<TheatreVisualGenerationInput> {
    @Override
    public VisualGenerationRequest build(TheatreVisualGenerationInput source) {
        if (source == null || source.context() == null || source.context().unit() == null) {
            throw new IllegalArgumentException("Falta el contexto teatral para generar la imagen.");
        }
        TheatreImageGenerationUnit unit = source.context().unit();
        LinkedHashMap<String, String> metadata = new LinkedHashMap<>();
        put(metadata, "consumer", "theatre");
        put(metadata, "actId", unit.actId());
        put(metadata, "sceneId", unit.sceneId());
        put(metadata, "interventionId", unit.interventionId());
        put(metadata, "segmentId", unit.segmentId());
        return new VisualGenerationRequest(
                source.prompt(),
                source.negativePrompt(),
                new VisualReferenceBundle(source.context().conditioningReferences()),
                source.width(),
                source.height(),
                source.profile(),
                source.computeBinding(),
                source.outputTarget(),
                Map.copyOf(metadata));
    }

    private static void put(Map<String, String> target, String key, String value) {
        String clean = value == null ? "" : value.strip();
        if (!clean.isBlank()) {
            target.put(key, clean);
        }
    }
}

package com.marcosmoreiradev.docupodcaststudio.application.documentstudy;

import com.marcosmoreiradev.docupodcaststudio.application.visual.VisualGenerationContextProvider;
import com.marcosmoreiradev.docupodcaststudio.application.visual.VisualGenerationRequest;

import java.util.LinkedHashMap;
import java.util.Map;

/** Documentary adapter reserved for paragraph, table and source-material illustration. */
public final class DocumentStudyVisualGenerationContextProvider
        implements VisualGenerationContextProvider<DocumentStudyVisualGenerationInput> {
    @Override
    public VisualGenerationRequest build(DocumentStudyVisualGenerationInput source) {
        if (source == null) {
            throw new IllegalArgumentException("Falta el contexto documental para generar la imagen.");
        }
        LinkedHashMap<String, String> metadata = new LinkedHashMap<>();
        metadata.put("consumer", "document-study");
        if (source.metadata() != null) {
            metadata.putAll(source.metadata());
        }
        return new VisualGenerationRequest(
                source.prompt(), source.negativePrompt(), source.references(),
                source.width(), source.height(), source.profile(), source.computeBinding(),
                source.outputTarget(), Map.copyOf(metadata));
    }
}

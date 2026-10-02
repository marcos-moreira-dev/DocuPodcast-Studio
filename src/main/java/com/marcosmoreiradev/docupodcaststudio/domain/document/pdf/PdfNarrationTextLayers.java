package com.marcosmoreiradev.docupodcaststudio.domain.document.pdf;

import java.util.Objects;

/** Literal evidence, optional interpretation and final narratable text kept apart. */
public record PdfNarrationTextLayers(
        String literalText,
        String interpretationText,
        String narrationText,
        PdfSemanticTextLayer narrationSourceLayer
) {
    public PdfNarrationTextLayers {
        literalText = normalize(literalText);
        interpretationText = normalize(interpretationText);
        narrationText = normalize(narrationText);
        narrationSourceLayer = Objects.requireNonNull(
                narrationSourceLayer, "narrationSourceLayer");
        if (narrationText.isBlank()) {
            throw new IllegalArgumentException("narrationText must not be blank");
        }
        if (narrationSourceLayer == PdfSemanticTextLayer.LITERAL
                && literalText.isBlank()) {
            throw new IllegalArgumentException(
                    "Literal narration requires literal evidence");
        }
        if (narrationSourceLayer == PdfSemanticTextLayer.INTERPRETATION
                && interpretationText.isBlank()) {
            throw new IllegalArgumentException(
                    "Interpretive narration requires interpretation text");
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}

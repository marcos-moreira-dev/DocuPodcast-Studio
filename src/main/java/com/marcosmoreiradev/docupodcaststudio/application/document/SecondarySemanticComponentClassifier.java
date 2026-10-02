package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfDerivedTreatmentKind;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfNarratability;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegion;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.SecondarySemanticComponentKind;

import java.util.Locale;

/** Classifies PDF evidence independently from the user's narration policy. */
public final class SecondarySemanticComponentClassifier {
    public SecondarySemanticComponentKind classify(PdfRegion region) {
        if (region == null) return SecondarySemanticComponentKind.NONE;
        return switch (region.effectiveType()) {
            case TABLE -> SecondarySemanticComponentKind.TABLE;
            case MATH -> SecondarySemanticComponentKind.EQUATION;
            case IMAGE -> SecondarySemanticComponentKind.IMAGE;
            case UNKNOWN -> region.effectiveNarratability() == PdfNarratability.NARRATABLE
                    ? SecondarySemanticComponentKind.NONE
                    : SecondarySemanticComponentKind.EXTRA;
            default -> SecondarySemanticComponentKind.NONE;
        };
    }

    public SecondarySemanticComponentKind classifySemanticType(String value) {
        String type = value == null ? "" : value.strip().toUpperCase(Locale.ROOT);
        return switch (type) {
            case "TABLE" -> SecondarySemanticComponentKind.TABLE;
            case "FORMULA", "MATH", "EQUATION", "INLINE_FORMULA", "BLOCK_FORMULA" ->
                    SecondarySemanticComponentKind.EQUATION;
            case "FIGURE", "DIAGRAM", "GRAPH", "IMAGE" ->
                    SecondarySemanticComponentKind.IMAGE;
            case "UNKNOWN", "TECHNICAL_REGION", "GIBBERISH" ->
                    SecondarySemanticComponentKind.EXTRA;
            default -> SecondarySemanticComponentKind.NONE;
        };
    }

    public SecondarySemanticComponentKind classify(DocumentTechnicalElement.Type type) {
        if (type == null) return SecondarySemanticComponentKind.NONE;
        return switch (type) {
            case TABLE -> SecondarySemanticComponentKind.TABLE;
            case FORMULA, INLINE_FORMULA, BLOCK_FORMULA ->
                    SecondarySemanticComponentKind.EQUATION;
            case FIGURE, GRAPH -> SecondarySemanticComponentKind.IMAGE;
            case TECHNICAL_REGION -> SecondarySemanticComponentKind.EXTRA;
        };
    }

    public SecondarySemanticComponentKind classify(PdfDerivedTreatmentKind kind) {
        if (kind == null) return SecondarySemanticComponentKind.NONE;
        return switch (kind) {
            case TABLE_STRUCTURE, TABLE_NARRATION, SMALL_TABLE_NARRATION ->
                    SecondarySemanticComponentKind.TABLE;
            case MATHEMATICAL_READING -> SecondarySemanticComponentKind.EQUATION;
            case IMAGE_DESCRIPTION -> SecondarySemanticComponentKind.IMAGE;
            case LIGHTWEIGHT_LANGUAGE_MODEL, NARRATABILITY_REVIEW ->
                    SecondarySemanticComponentKind.EXTRA;
            default -> SecondarySemanticComponentKind.NONE;
        };
    }
}

package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.*;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.SecondarySemanticComponentKind;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class SecondarySemanticComponentClassifierTest {
    private final SecondarySemanticComponentClassifier classifier =
            new SecondarySemanticComponentClassifier();

    @Test
    void classifiesBeforeAnyReadingPolicyIsApplied() {
        assertEquals(SecondarySemanticComponentKind.TABLE,
                classifier.classify(region(PdfRegionType.TABLE,
                        PdfNarratability.NON_NARRATABLE)));
        assertEquals(SecondarySemanticComponentKind.EQUATION,
                classifier.classify(region(PdfRegionType.MATH,
                        PdfNarratability.NON_NARRATABLE)));
        assertEquals(SecondarySemanticComponentKind.IMAGE,
                classifier.classify(region(PdfRegionType.IMAGE,
                        PdfNarratability.NON_NARRATABLE)));
        assertEquals(SecondarySemanticComponentKind.EXTRA,
                classifier.classify(region(PdfRegionType.UNKNOWN,
                        PdfNarratability.UNCERTAIN)));
        assertEquals(SecondarySemanticComponentKind.NONE,
                classifier.classify(region(PdfRegionType.PARAGRAPH,
                        PdfNarratability.NARRATABLE)));
        assertEquals(SecondarySemanticComponentKind.NONE,
                classifier.classify(region(PdfRegionType.HEADER,
                        PdfNarratability.NON_NARRATABLE)));
    }

    private static PdfRegion region(
            PdfRegionType type, PdfNarratability narratability) {
        return new PdfRegion("R-" + type, 1, 10, 10, 100, 40,
                0, 0, "evidence", type, narratability, List.of(),
                new PdfRegionEvidence(PdfRegionOrigin.NATIVE_TEXT, 1,
                        "x", "p", "g", "c"),
                PdfRegionOverride.empty(), Map.of(), 1);
    }
}

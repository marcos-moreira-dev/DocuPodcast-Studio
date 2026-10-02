package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class PdfTextVisualBoundsResolverTest {
    private final PdfTextVisualBoundsResolver resolver =
            new PdfTextVisualBoundsResolver();
    private final PdfPageRegion semantic = new PdfPageRegion(
            1, 40, 80, 580, 320, 612, 792);

    @Test
    void shortLineUsesRightmostOcrWordInsteadOfSemanticColumnWidth() {
        var result = resolver.resolve(region(PdfRegionType.PARAGRAPH,
                Map.of(PdfTextVisualBoundsResolver.OCR_WORD_BBOXES_ATTRIBUTE,
                        "72,100,110,114;116,100,180,114")), semantic);

        assertEquals(PdfTextVisualBoundsResolver.Source.OCR_WORDS, result.source());
        assertEquals(72.0, result.tightTextBBox().xMinPoints());
        assertEquals(180.0, result.tightTextBBox().xMaxPoints());
        assertEquals(2, result.matchedWordCount());
        assertEquals(580.0, semantic.xMaxPoints(),
                "canonical semantic bbox is not mutated");
    }

    @Test
    void multilineUnionEndsAtTheLongestRealLine() {
        var result = resolver.resolve(region(PdfRegionType.PARAGRAPH,
                Map.of(PdfTextVisualBoundsResolver.OCR_WORD_BBOXES_ATTRIBUTE,
                        "72,100,900,114;72,130,850,144;72,160,500,174")),
                new PdfPageRegion(1, 40, 80, 1000, 240, 1100, 792));

        assertEquals(900.0, result.tightTextBBox().xMaxPoints());
        assertEquals(100.0, result.tightTextBBox().yMinPoints());
        assertEquals(174.0, result.tightTextBBox().yMaxPoints());
    }

    @Test
    void missingOrMalformedWordsUseSemanticFallback() {
        assertEquals(PdfTextVisualBoundsResolver.Source.SEMANTIC_FALLBACK,
                resolver.resolve(region(PdfRegionType.PARAGRAPH, Map.of()), semantic).source());
        assertEquals(semantic, resolver.resolve(region(PdfRegionType.PARAGRAPH,
                Map.of(PdfTextVisualBoundsResolver.OCR_WORD_BBOXES_ATTRIBUTE,
                        "not-a-box")), semantic).tightTextBBox());
    }

    @Test
    void specializedAndMixedRegionsNeverUseWordEnvelope() {
        for (PdfRegionType type : List.of(PdfRegionType.IMAGE,
                PdfRegionType.TABLE, PdfRegionType.CODE, PdfRegionType.MATH)) {
            var result = resolver.resolve(region(type,
                    Map.of(PdfTextVisualBoundsResolver.OCR_WORD_BBOXES_ATTRIBUTE,
                            "72,100,180,114")), semantic);
            assertEquals(PdfTextVisualBoundsResolver.Source.SPECIALIZED, result.source());
            assertEquals(semantic, result.tightTextBBox());
        }
        var mixed = resolver.resolve(region(PdfRegionType.PARAGRAPH,
                Map.of("contentRoute", "VLM_MIXED",
                        PdfTextVisualBoundsResolver.OCR_WORD_BBOXES_ATTRIBUTE,
                        "72,100,180,114")), semantic);
        assertEquals(PdfTextVisualBoundsResolver.Source.SPECIALIZED, mixed.source());
    }

    private static PdfRegion region(PdfRegionType type, Map<String, String> attributes) {
        return new PdfRegion("R", 1, 40, 80, 580, 320, 0, 0,
                "Texto", type, PdfNarratability.NARRATABLE, List.of(),
                new PdfRegionEvidence(PdfRegionOrigin.VLM_SEMANTIC, 0.95,
                        "", "", "", ""), PdfRegionOverride.empty(),
                attributes, 1);
    }
}

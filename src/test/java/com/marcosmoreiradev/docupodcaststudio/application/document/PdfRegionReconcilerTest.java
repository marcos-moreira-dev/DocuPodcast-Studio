package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfNarratability;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegion;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionEvidence;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionOrigin;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionOverride;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PdfRegionReconcilerTest {
    @Test
    void reprocessingKeepsStableIdAndIndependentOverrides() {
        PdfRegion previous = region("stable-id", 10, 20, 500, 80,
                "Texto original del párrafo.",
                new PdfRegionOverride("Texto corregido.", PdfRegionType.HEADING,
                        PdfNarratability.NARRATABLE, 7));
        PdfRegion candidate = region("generated-id", 12, 21, 502, 82,
                "Texto original del parrafo.", PdfRegionOverride.empty());

        PdfRegion reconciled = new PdfRegionReconciler()
                .reconcile(List.of(previous), List.of(candidate))
                .getFirst();

        assertEquals("stable-id", reconciled.id());
        assertEquals("Texto corregido.", reconciled.effectiveText());
        assertEquals(PdfRegionType.HEADING, reconciled.effectiveType());
        assertEquals(7, reconciled.effectiveReadingOrder());
    }

    @Test
    void splitFollowerBecomesUncertainWhenReviewedRegionWasSplit() {
        PdfRegion previous = region("reviewed", 10, 20, 500, 120,
                "Una región revisada que luego se divide en dos párrafos.",
                new PdfRegionOverride(null, null, PdfNarratability.NARRATABLE, null));
        PdfRegion first = region("new-a", 10, 20, 500, 68,
                "Una región revisada que luego", PdfRegionOverride.empty());
        PdfRegion second = region("new-b", 10, 70, 500, 120,
                "se divide en dos párrafos.", PdfRegionOverride.empty());

        List<PdfRegion> reconciled = new PdfRegionReconciler()
                .reconcile(List.of(previous), List.of(first, second));

        assertEquals("reviewed", reconciled.getFirst().id());
        assertEquals(PdfNarratability.UNCERTAIN, reconciled.get(1).automaticNarratability());
        assertTrue(reconciled.get(1).reasons().contains("possible-split-from-manually-reviewed-region"));
    }

    private static PdfRegion region(String id,
                                    double x1,
                                    double y1,
                                    double x2,
                                    double y2,
                                    String text,
                                    PdfRegionOverride override) {
        return new PdfRegion(id, 1, x1, y1, x2, y2, 0, 0, text,
                PdfRegionType.PARAGRAPH, PdfNarratability.NARRATABLE, List.of("test"),
                new PdfRegionEvidence(PdfRegionOrigin.OCR_LOCAL, 0.9,
                        "test", "test", "test", "test"),
                override, Map.of(), 1);
    }
}

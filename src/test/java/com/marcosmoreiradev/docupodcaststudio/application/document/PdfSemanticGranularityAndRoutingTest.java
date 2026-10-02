package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfContentRoute;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfNarratability;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfPageRole;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class PdfSemanticGranularityAndRoutingTest {
    private final PdfSemanticGranularityValidator validator =
            new PdfSemanticGranularityValidator();

    @Test
    void routesCleanProseAwayFromVlmAndKeepsMixedOrVisualContentOnVlm() {
        assertEquals(PdfContentRoute.OCR_SAFE, PdfRegionLayoutSemantics.route(
                PdfRegionType.PARAGRAPH, "Un parrafo ordinario y legible."));
        assertEquals(PdfContentRoute.VLM_MIXED, PdfRegionLayoutSemantics.route(
                PdfRegionType.PARAGRAPH, "Cuando x = 0 aparece el limite."));
        assertEquals(PdfContentRoute.VLM_VISUAL, PdfRegionLayoutSemantics.route(
                PdfRegionType.IMAGE, "curva"));
        assertEquals(PdfContentRoute.VLM_STRUCTURED, PdfRegionLayoutSemantics.route(
                PdfRegionType.TABLE, "A ; B"));
        assertEquals(PdfContentRoute.VLM_STRUCTURED, PdfRegionLayoutSemantics.route(
                PdfRegionType.CODE, "for (int i = 0; i < n; i++)"));
    }

    @Test
    void rejectsAFullPageTextualPlaybackLeafButAllowsAContainer() {
        var giantLeaf = element("giant", PdfRegionType.PARAGRAPH,
                0, 0, 1000, 1000, Map.of("regionRole", "LEAF",
                        "playbackTarget", "true"));
        assertThrows(IllegalArgumentException.class,
                () -> validator.validateCanonical(page(giantLeaf)));

        var container = element("section", PdfRegionType.SIDEBAR,
                0, 0, 1000, 1000, Map.of("regionRole", "CONTAINER",
                        "playbackTarget", "false"));
        validator.validateCanonical(page(container));
    }

    @Test
    void smallSidebarMayRemainALeafWhileCompoundLargeSidebarNeedsRefinement() {
        var small = element("small", PdfRegionType.SIDEBAR,
                100, 100, 500, 260, Map.of());
        assertEquals(0, validator.refinementCandidates(page(small), null).size());

        var large = element("large", PdfRegionType.SIDEBAR,
                30, 30, 970, 850, Map.of());
        var heading = element("heading", PdfRegionType.HEADING,
                80, 80, 900, 140, Map.of());
        var paragraph = element("paragraph", PdfRegionType.PARAGRAPH,
                80, 180, 900, 330, Map.of());
        var math = element("math", PdfRegionType.MATH,
                250, 400, 750, 500, Map.of());
        PdfSemanticPageAnalysis compound = new PdfSemanticPageAnalysis(
                1, "es", PdfPageRole.CONTENT,
                List.of(large, heading, paragraph, math), 1.0, List.of());
        assertEquals(List.of(large),
                validator.refinementCandidates(compound, null));
    }

    private static PdfSemanticPageAnalysis page(PdfSemanticPageAnalysis.Element value) {
        return new PdfSemanticPageAnalysis(1, "es", PdfPageRole.CONTENT,
                List.of(value), 1.0, List.of());
    }

    private static PdfSemanticPageAnalysis.Element element(String id, PdfRegionType type,
            double x1, double y1, double x2, double y2, Map<String, String> attrs) {
        return new PdfSemanticPageAnalysis.Element(id, 0, type,
                new PdfSemanticPageAnalysis.NormalizedBox(x1, y1, x2, y2),
                "contenido", "", PdfNarratability.NARRATABLE, 1.0,
                List.of(), attrs);
    }
}

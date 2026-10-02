package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfNarratability;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfPageMap;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfPageNodeKind;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfPagePreparationStatus;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegion;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionEvidence;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionOrigin;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionOverride;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PreparedPdfPage;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BuildPdfPageMapUseCaseTest {
    private final BuildPdfPageMapUseCase builder = new BuildPdfPageMapUseCase();

    @Test
    void buildsHierarchyGeometryEvidenceReviewAndBindingsWithoutReplacingLiteral() {
        PdfRegion region = region("R1", "Primera oración. Segunda oración.", PdfRegionOverride.empty());

        PdfPageMap pageMap = builder.build(page(3, region), null);

        assertEquals(1, pageMap.nodes(PdfPageNodeKind.PAGE).size());
        assertEquals(1, pageMap.nodes(PdfPageNodeKind.BLOCK).size());
        assertEquals(2, pageMap.nodes(PdfPageNodeKind.LINE).size());
        assertEquals(2, pageMap.nodes(PdfPageNodeKind.SENTENCE).size());
        assertEquals(1, pageMap.narrationBindings().size());
        assertEquals(3, pageMap.sourcePageRevision());
        assertEquals("Primera oración. Segunda oración.",
                pageMap.nodes(PdfPageNodeKind.BLOCK).getFirst().text().literalText());
        assertFalse(pageMap.nodes(PdfPageNodeKind.BLOCK).getFirst().reviewState().humanOverride());
        assertEquals(2, pageMap.nodes(PdfPageNodeKind.SENTENCE).getFirst().geometryParts().size() == 0 ? 0 :
                pageMap.nodes(PdfPageNodeKind.SENTENCE).stream().mapToInt(node -> node.geometryParts().size()).sum());
    }

    @Test
    void reconcilesStableIdentityAndPreservesHumanCorrection() {
        PdfPageMap original = builder.build(page(1,
                region("LEGACY-1", "Texto científicamente estable.", PdfRegionOverride.empty())), null);
        String originalBlockId = original.nodes(PdfPageNodeKind.BLOCK).getFirst().id();
        PdfRegionOverride correction = new PdfRegionOverride(
                "Texto científicamente corregido por una persona.", PdfRegionType.PARAGRAPH,
                PdfNarratability.NARRATABLE, 0);

        PdfPageMap rebuilt = builder.build(page(2,
                region("NEW-ID", "Texto científicamente estable.", correction)), original);

        assertEquals(originalBlockId, rebuilt.nodes(PdfPageNodeKind.BLOCK).getFirst().id());
        assertTrue(rebuilt.nodes(PdfPageNodeKind.BLOCK).getFirst().reviewState().humanOverride());
        assertEquals("Texto científicamente corregido por una persona.",
                rebuilt.nodes(PdfPageNodeKind.BLOCK).getFirst().text().literalText());
    }

    @Test
    void semanticObjectOwnsAllTableCellsAndExposesTheirCombinedText() {
        List<PdfRegion> cells = List.of(
                positioned("C1", 90, 110, 180, 130, "Región"),
                positioned("C2", 245, 110, 290, 130, "Área"),
                positioned("C3", 330, 110, 485, 130, "Razón geométrica"),
                positioned("C4", 90, 140, 180, 160, "Triángulo interior"),
                positioned("C5", 245, 140, 290, 160, "un medio seno de equis"),
                positioned("C6", 330, 140, 485, 160, "Base por altura"));
        PreparedPdfPage source = new PreparedPdfPage(
                PreparedPdfPage.CURRENT_SCHEMA_VERSION, 1, 612, 792,
                PdfPagePreparationStatus.READY, 1, cells, List.of(), "");

        PdfPageMap pageMap = builder.build(source, null);

        var table = pageMap.nodes(PdfPageNodeKind.VISUAL_OBJECT).stream()
                .filter(node -> "TABLE".equals(node.semanticType()))
                .findFirst().orElseThrow();
        assertEquals(5, table.childIds().size());
        assertTrue(table.text().literalText().contains("Región"));
        assertTrue(table.text().literalText().contains("Base por altura"));
        assertEquals(5, table.childIds().stream().map(pageMap::node)
                .flatMap(java.util.Optional::stream)
                .map(node -> node.legacyRegionId()).distinct().count());
    }

    private static PreparedPdfPage page(long revision, PdfRegion region) {
        return new PreparedPdfPage(PreparedPdfPage.CURRENT_SCHEMA_VERSION, 1, 612, 792,
                PdfPagePreparationStatus.READY, revision, List.of(region), List.of(), "");
    }

    private static PdfRegion region(String id, String text, PdfRegionOverride override) {
        int second = text.indexOf("Segunda");
        Map<String, String> attributes = second < 0 ? Map.of() : Map.of(
                "lineBboxes", "72,100,420,112;72,130,420,142",
                "lineCharRanges", "0-" + second + ";" + second + "-" + text.length());
        return new PdfRegion(id, 1, 72, 100, 420, 142, 0, 0, text,
                PdfRegionType.PARAGRAPH, PdfNarratability.NARRATABLE, List.of("test"),
                new PdfRegionEvidence(PdfRegionOrigin.NATIVE_TEXT, 0.98,
                        "extractor", "parser", "grouping", "classifier"),
                override, attributes, 1);
    }

    private static PdfRegion positioned(String id, double x1, double y1,
                                        double x2, double y2, String text) {
        return new PdfRegion(id, 1, x1, y1, x2, y2, 0, 0, text,
                PdfRegionType.PARAGRAPH, PdfNarratability.NARRATABLE,
                List.of("test"), new PdfRegionEvidence(
                PdfRegionOrigin.NATIVE_TEXT, 0.98, "extractor", "parser",
                "grouping", "classifier"), PdfRegionOverride.empty(),
                Map.of(), 1);
    }
}

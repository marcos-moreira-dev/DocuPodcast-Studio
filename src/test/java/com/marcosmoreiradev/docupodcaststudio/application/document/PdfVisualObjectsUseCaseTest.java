package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfNarratability;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfPageGeometry;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfPageNodeKind;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfPagePreparationStatus;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegion;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionEvidence;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionOrigin;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionOverride;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PreparedPdfPage;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.document.JsonPdfPageMapRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PdfVisualObjectsUseCaseTest {
    @TempDir Path temp;

    @Test
    void detectsCompleteFigureSubordinatesLabelsAndHighlightsWholeObject() throws Exception {
        PreparedPdfPage page = page(1);
        var proposals = new DetectPdfVisualObjectsUseCase().detect(page);
        var figure = proposals.getFirst();

        assertEquals(PdfVisualObjectProposal.Type.FIGURE, figure.type());
        assertEquals(List.of("LABEL"), figure.internalLabelRegionIds());
        assertEquals("CAPTION", figure.captionRegionId());
        assertTrue(figure.geometry().xMinPoints() <= 92);
        assertTrue(figure.geometry().yMinPoints() <= 142);

        InMemoryPreparedPdfDocumentRepository v3 = new InMemoryPreparedPdfDocumentRepository();
        PreparedPdfWorkspaceRef workspace = PreparedPdfTestFixtures.workspace(v3, temp, 1);
        v3.savePage(temp, page);
        ResolvePdfPageMapUseCase resolver = new ResolvePdfPageMapUseCase(v3,
                new JsonPdfPageMapRepository(), new BuildPdfPageMapUseCase(), true);
        var map = resolver.resolve(workspace, 1).pageMap();
        assertEquals(1, map.nodes(PdfPageNodeKind.VISUAL_OBJECT).size());
        assertEquals(1, map.nodes(PdfPageNodeKind.INTERNAL_LABEL).size());
        assertEquals(List.of("PROSE"), map.nodes(PdfPageNodeKind.BLOCK).stream()
                .map(node -> node.legacyRegionId()).toList());

        PdfVisualReadingProjection projection = new BuildPdfVisualReadingProjectionUseCase(v3, resolver).build(workspace);
        assertEquals(toGeometry(figure.geometry()),
                toGeometry(projection.highlightForRegion("LABEL").orElseThrow().region()));
        assertFalse(projection.targets().stream().anyMatch(target -> target.regionId().equals("LABEL")));
    }

    @Test
    void manualGeometryTypeAndDecisionSurviveReanalysis() throws Exception {
        InMemoryPreparedPdfDocumentRepository v3 = new InMemoryPreparedPdfDocumentRepository();
        PreparedPdfWorkspaceRef workspace = PreparedPdfTestFixtures.workspace(v3, temp, 1);
        v3.savePage(temp, page(1));
        JsonPdfPageMapRepository maps = new JsonPdfPageMapRepository();
        ResolvePdfPageMapUseCase resolver = new ResolvePdfPageMapUseCase(v3, maps, new BuildPdfPageMapUseCase(), true);
        var object = resolver.resolve(workspace, 1).pageMap().nodes(PdfPageNodeKind.VISUAL_OBJECT).getFirst();
        PdfPageGeometry corrected = new PdfPageGeometry(80, 130, 530, 430, 612, 792, PdfPageGeometry.CANONICAL_SPACE);

        new UpdatePdfVisualObjectUseCase(maps).update(workspace, 1, object.id(), corrected,
                PdfVisualObjectProposal.Type.DIAGRAM, UpdatePdfVisualObjectUseCase.Decision.CONFIRMED);
        v3.savePage(temp, page(2));
        var rebuilt = resolver.resolve(workspace, 1).pageMap().node(object.id()).orElseThrow();

        assertEquals(corrected, rebuilt.geometry());
        assertEquals("DIAGRAM", rebuilt.semanticType());
        assertEquals("CONFIRMED", rebuilt.reviewState().state());
        assertTrue(rebuilt.reviewState().humanOverride());
    }

    private static PdfPageGeometry toGeometry(PdfPageRegion region) {
        return new PdfPageGeometry(region.xMinPoints(), region.yMinPoints(), region.xMaxPoints(),
                region.yMaxPoints(), region.pageWidthPoints(), region.pageHeightPoints(), PdfPageGeometry.CANONICAL_SPACE);
    }

    private static PreparedPdfPage page(long revision) {
        return new PreparedPdfPage(PreparedPdfPage.CURRENT_SCHEMA_VERSION, 1, 612, 792,
                PdfPagePreparationStatus.READY, revision, List.of(
                region("PROSE", 72, 60, 540, 100, "La energía fluye en el sistema.", PdfRegionType.PARAGRAPH),
                region("IMAGE", 100, 150, 500, 350, "", PdfRegionType.IMAGE),
                region("LABEL", 230, 210, 300, 225, "Flujo Q", PdfRegionType.UNKNOWN),
                region("CAPTION", 110, 390, 490, 420, "Figura 1. Transferencia térmica.", PdfRegionType.CAPTION)),
                List.of(), "");
    }

    private static PdfRegion region(String id, double x1, double y1, double x2, double y2,
                                    String text, PdfRegionType type) {
        return new PdfRegion(id, 1, x1, y1, x2, y2, 0,
                switch (id) { case "PROSE" -> 0; case "IMAGE" -> 1; case "LABEL" -> 2; default -> 3; },
                text, type, type == PdfRegionType.PARAGRAPH ? PdfNarratability.NARRATABLE : PdfNarratability.UNCERTAIN,
                List.of(), new PdfRegionEvidence(PdfRegionOrigin.NATIVE_TEXT, 0.9, "e", "p", "g", "c"),
                PdfRegionOverride.empty(), Map.of(), 1);
    }
}

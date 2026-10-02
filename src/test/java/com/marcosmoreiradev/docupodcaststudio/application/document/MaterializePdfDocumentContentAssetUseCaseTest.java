package com.marcosmoreiradev.docupodcaststudio.application.document;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class MaterializePdfDocumentContentAssetUseCaseTest {
    @Test
    void clipsTopContextAtNearestSiblingWithoutChangingSourceBounds() {
        DocumentContentItem owner = item("MATH", 100, 200, 300, 260);
        DocumentContentItem top = item("P-TOP", 80, 120, 320, 190);
        var decision = MaterializePdfDocumentContentAssetUseCase.siblingAwareCrop(
                owner.pdfAnchor().orElseThrow(), List.of(top, owner), 30);
        assertEquals(195, decision.finalCrop().yMin());
        assertEquals(100, decision.sourceBounds().xMin());
        assertEquals(200, owner.pdfAnchor().orElseThrow().roi().yMin());
    }

    @Test
    void clipsBottomContextAtNearestSiblingAndKeepsWholeTable() {
        DocumentContentItem table = item("TABLE", 60, 220, 540, 500);
        DocumentContentItem bottom = item("P-BOTTOM", 50, 515, 550, 590);
        var decision = MaterializePdfDocumentContentAssetUseCase.siblingAwareCrop(
                table.pdfAnchor().orElseThrow(), List.of(table, bottom), 40);
        assertEquals(507.5, decision.finalCrop().yMax());
        assertEquals(180, decision.finalCrop().yMin());
        assertEquals(60, decision.sourceBounds().xMin());
        assertEquals(540, decision.sourceBounds().xMax());
    }

    @Test
    void imageCaptureKeepsSemanticHeightAndUsesSafePageWidth() {
        var anchor = new PdfContentAnchor(1, 600, 800,
                new DocumentContentRectangle(150, 200, 450, 500),
                List.of("REG-IMAGE"), 1, "visual-image");
        var image = new DocumentContentItem("IMAGE", DocumentContentKind.IMAGE,
                "Figura", "Speech", List.of("SEG-IMAGE"), List.of("REG-IMAGE"),
                "content-image", 1, anchor);

        DocumentContentRectangle capture =
                MaterializePdfDocumentContentAssetUseCase.sourceCaptureBounds(image, anchor);

        assertEquals(36, capture.xMin());
        assertEquals(564, capture.xMax());
        assertEquals(200, capture.yMin());
        assertEquals(512, capture.yMax());
        assertEquals(150, anchor.roi().xMin(), "La geometria canonica no cambia");
    }

    @Test
    void unreliableCanonicalGeometryFallsBackToTheWholeSamePage() {
        var semantic = new DocumentContentRectangle(30, 40, 300, 90);
        var anchor = new PdfContentAnchor(3, 600, 800, semantic,
                List.of("REG-BAD"), Map.of("REG-BAD", semantic),
                Map.of("REG-BAD", "Texto correcto"), List.of("REG-BAD"),
                2, "visual-bad");

        var decision = MaterializePdfDocumentContentAssetUseCase.fullPageFallback(anchor);

        assertEquals(semantic, decision.sourceBounds());
        assertEquals(new DocumentContentRectangle(0, 0, 600, 800), decision.finalCrop());
        assertEquals("page-fallback:unreliable-source-geometry", decision.cropSource());
    }

    @Test
    void mixedParagraphKeepsDisplayFormulaBelowTheTextBaseline() {
        var semantic = new DocumentContentRectangle(0, 286, 583, 311);
        var anchor = new PdfContentAnchor(3, 595, 842, semantic,
                List.of("REG-MIXED"), Map.of("REG-MIXED", semantic),
                Map.of("REG-MIXED", "Esta conclusion explica...\n\\sin x \\approx x."),
                List.of(), 1, "visual-mixed");
        var mixed = new DocumentContentItem("MIXED", DocumentContentKind.PROSE,
                "Mixto", "Speech", List.of("SEG-MIXED"), List.of("REG-MIXED"),
                "content-mixed", 1, anchor);

        DocumentContentRectangle capture =
                MaterializePdfDocumentContentAssetUseCase.sourceCaptureBounds(mixed, anchor);

        assertEquals(268, capture.yMin());
        assertEquals(329, capture.yMax());
        assertEquals(semantic, anchor.roi(), "La bbox canonica no se modifica");
    }

    private static DocumentContentItem item(String id, double x1, double y1,
                                            double x2, double y2) {
        var anchor = new PdfContentAnchor(1, 600, 800,
                new DocumentContentRectangle(x1, y1, x2, y2),
                List.of("REG-" + id), 1, "visual-" + id);
        return new DocumentContentItem(id, DocumentContentKind.EQUATION, id, id,
                List.of("SEG-" + id), List.of("REG-" + id),
                "content-" + id, 1, anchor);
    }
}

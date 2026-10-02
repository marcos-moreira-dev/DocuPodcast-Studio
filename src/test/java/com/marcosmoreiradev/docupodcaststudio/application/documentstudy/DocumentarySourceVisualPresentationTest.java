package com.marcosmoreiradev.docupodcaststudio.application.documentstudy;

import com.marcosmoreiradev.docupodcaststudio.application.document.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class DocumentarySourceVisualPresentationTest {
    @Test
    void allPdfCardsDeclareTheirPendingOriginalVisualBeforeAnySelection() {
        List<DocumentContentItem> items = List.of(pdfItem("A", 1), pdfItem("B", 2), pdfItem("C", 3));

        var beforeSelection = items.stream()
                .map(item -> DocumentarySourceVisualPresentation.of(item, false)).toList();
        var afterSelectingOnlyMiddleCard = items.stream()
                .map(item -> DocumentarySourceVisualPresentation.of(item, item.contentId().equals("B"))).toList();

        assertEquals(List.of("Imagen original PDF pendiente", "Imagen original PDF pendiente",
                "Imagen original PDF pendiente"), beforeSelection.stream().map(
                DocumentarySourceVisualPresentation::label).toList());
        assertEquals(DocumentarySourceVisualPresentation.State.VISUAL_AVAILABLE_NOT_MATERIALIZED,
                afterSelectingOnlyMiddleCard.getFirst().state());
        assertEquals(DocumentarySourceVisualPresentation.State.VISUAL_READY,
                afterSelectingOnlyMiddleCard.get(1).state());
        assertEquals(DocumentarySourceVisualPresentation.State.VISUAL_AVAILABLE_NOT_MATERIALIZED,
                afterSelectingOnlyMiddleCard.getLast().state());
    }

    @Test
    void textRenderPreviewDoesNotClaimOrMaterializeAPdfCrop() {
        DocumentContentItem text = new DocumentContentItem("TEXT", DocumentContentKind.PROSE,
                "Texto", "Texto", List.of("SEG-TEXT"), List.of("REG-TEXT"),
                List.of(), "FP-TEXT", 1, DocumentPresentationMode.TEXT_RENDER,
                new PdfContentAnchor(1, 612, 792,
                        new DocumentContentRectangle(10, 20, 100, 80),
                        List.of("REG-TEXT"), 1, "VFP-TEXT"));

        var preview = DocumentarySourceVisualPresentation.of(text, false);

        assertEquals(DocumentarySourceVisualPresentation.State.NO_VISUAL, preview.state());
        assertEquals("TEXT_RENDER", preview.label());
    }

    private static DocumentContentItem pdfItem(String id, int page) {
        return new DocumentContentItem(id, DocumentContentKind.PROSE, id, "texto", List.of("SEG-" + id),
                List.of("REG-" + id), List.of(), "FP-" + id, 1,
                DocumentPresentationMode.SOURCE_CAPTURE,
                new PdfContentAnchor(page, 612, 792,
                        new DocumentContentRectangle(10, 20, 100, 80), List.of("REG-" + id), 1, "VFP-" + id));
    }
}

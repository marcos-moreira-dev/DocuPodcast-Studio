package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlockType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentFormat;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BuildPdfNativeTextLayerUseCaseTest {
    @Test
    void buildsNativeBboxLayersByPage() {
        ReadableDocument document = new ReadableDocument(
                "Libro",
                SourceDocumentFormat.PDF,
                Path.of("book.pdf"),
                List.of(
                        block("B0002", "Second line", "2", "20,30,120,50", "3"),
                        block("B0001", "First line", "1", "10,20,210,40", "3")));

        List<PdfTextLayer> layers = new BuildPdfNativeTextLayerUseCase().build(document);

        assertEquals(3, layers.size());
        assertEquals(PdfTextLayerOrigin.NATIVE_BBOX, layers.get(0).origin());
        assertEquals("First line", layers.get(0).lines().getFirst().text());
        assertEquals("10.000,20.000,210.000,40.000", layers.get(0).lines().getFirst().region().bbox());
        assertFalse(layers.get(0).lines().getFirst().tokens().isEmpty());
        assertEquals(PdfTextLayerOrigin.NATIVE_BBOX, layers.get(1).origin());
        assertEquals(PdfTextLayerOrigin.UNAVAILABLE, layers.get(2).origin());
        assertTrue(layers.get(2).warnings().getFirst().contains("sin texto nativo bbox"));
    }

    @Test
    void ignoresNonPdfAndBlocksWithoutBbox() {
        ReadableDocument nonPdf = new ReadableDocument(
                "Notas",
                SourceDocumentFormat.MARKDOWN,
                Path.of("notes.md"),
                List.of(DocumentBlock.of("B0001", DocumentBlockType.PARAGRAPH, "Hola", "")));
        ReadableDocument pdfWithoutBbox = new ReadableDocument(
                "PDF",
                SourceDocumentFormat.PDF,
                Path.of("book.pdf"),
                List.of(DocumentBlock.of("B0001", DocumentBlockType.PARAGRAPH, "Hola", "", Map.of(
                        "sourcePage", "1",
                        "sourcePageCount", "1",
                        "bboxUnits", ""))));

        assertTrue(new BuildPdfNativeTextLayerUseCase().build(nonPdf).isEmpty());
        List<PdfTextLayer> layers = new BuildPdfNativeTextLayerUseCase().build(pdfWithoutBbox);
        assertEquals(1, layers.size());
        assertEquals(PdfTextLayerOrigin.UNAVAILABLE, layers.getFirst().origin());
    }

    private static DocumentBlock block(String id, String text, String page, String bbox, String pageCount) {
        return DocumentBlock.of(id, DocumentBlockType.PARAGRAPH, text, "PDF bbox text", Map.of(
                "sourceFormat", "PDF",
                "sourcePage", page,
                "sourcePageCount", pageCount,
                "bbox", bbox,
                "bboxUnits", "pdf-points",
                "pageWidth", "612.000",
                "pageHeight", "792.000",
                "extractionMode", "poppler-bbox-layout",
                "confidence", "native-text"));
    }
}

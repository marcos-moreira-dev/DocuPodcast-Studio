package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlockType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentSentenceSplitter;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentFormat;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BuildPdfVisualReadingProjectionUseCaseTest {
    @Test
    void createsSentenceTargetsWhenLineGeometryExists() {
        DocumentBlock block = pdfBlock("B0001", "First sentence. Second sentence.", Map.of(
                "lineBboxes", "72,100,300,118;72,124,340,142",
                "lineCharRanges", "0-15;16-32"));
        PdfVisualReadingProjection projection = new BuildPdfVisualReadingProjectionUseCase(null).build(pdf(block));

        assertEquals(3, projection.targets().size());
        assertEquals(2, projection.targets().stream()
                .filter(target -> target.kind() == PdfVisualTextTargetKind.SENTENCE)
                .count());
        assertTrue(projection.targetAt(1, 80, 108).orElseThrow().text().startsWith("First"));
        assertTrue(projection.targetAt(1, 80, 132).orElseThrow().text().startsWith("Second"));
        assertTrue(projection.targetForRange(DocumentSentenceSplitter.split(block).getFirst().range()).isPresent());
    }

    @Test
    void fallsBackToBlockWhenOnlyParagraphBboxExists() {
        DocumentBlock block = pdfBlock("B0001", "First sentence. Second sentence.", Map.of());
        PdfVisualReadingProjection projection = new BuildPdfVisualReadingProjectionUseCase(null).build(pdf(block));

        assertEquals(1, projection.targets().size());
        assertEquals(PdfVisualTextTargetKind.BLOCK, projection.targetAt(1, 80, 108).orElseThrow().kind());
        assertFalse(projection.targetForRange(DocumentSentenceSplitter.split(block).getFirst().range()).isPresent());
        assertTrue(projection.highlightForBlock("B0001").isPresent());
    }

    @Test
    void skipsMathDominantTargets() {
        DocumentBlock math = pdfBlock("B0001", "x = a + b / 2", Map.of());
        PdfVisualReadingProjection projection = new BuildPdfVisualReadingProjectionUseCase(null).build(pdf(math));

        assertTrue(projection.targets().isEmpty());
        assertTrue(projection.highlightForBlock("B0001").isEmpty());
    }

    @Test
    void ignoresNativePdfBlocksEvenWhenTheyHaveBbox() {
        DocumentBlock nativeBlock = pdfBlock("B0001", "Native text should not be interactive.", Map.of(
                "ocr", "false",
                "nativeText", "true",
                "extractionMode", "pdfbox-text"));

        PdfVisualReadingProjection projection = new BuildPdfVisualReadingProjectionUseCase(null).build(pdf(nativeBlock));

        assertTrue(projection.targets().isEmpty());
        assertTrue(projection.highlightForBlock("B0001").isEmpty());
    }

    private static ReadableDocument pdf(DocumentBlock... blocks) {
        return new ReadableDocument("pdf", SourceDocumentFormat.PDF, Path.of("book.pdf"), List.of(blocks));
    }

    private static DocumentBlock pdfBlock(String id, String text, Map<String, String> extraMetadata) {
        java.util.LinkedHashMap<String, String> metadata = new java.util.LinkedHashMap<>();
        metadata.put("sourcePage", "1");
        metadata.put("sourcePageCount", "1");
        metadata.put("bbox", "72,100,420,142");
        metadata.put("bboxUnits", "pdf-points");
        metadata.put("pageWidth", "612");
        metadata.put("pageHeight", "792");
        metadata.put("nativeText", "false");
        metadata.put("ocr", "true");
        metadata.put("extractionMode", "ocr-local");
        metadata.putAll(extraMetadata);
        return DocumentBlock.of(id, DocumentBlockType.PARAGRAPH, text, "PDF OCR", metadata);
    }
}

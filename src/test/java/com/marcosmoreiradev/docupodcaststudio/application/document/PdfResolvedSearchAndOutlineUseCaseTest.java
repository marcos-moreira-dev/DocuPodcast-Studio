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

final class PdfResolvedSearchAndOutlineUseCaseTest {
    @Test
    void resolvedTextLayerUsesOcrWhenNativePageIsUnavailable() {
        BuildPdfResolvedTextLayerUseCase useCase = new BuildPdfResolvedTextLayerUseCase(
                new BuildPdfNativeTextLayerUseCase(),
                new BuildPdfOcrTextLayerUseCase(new FakeOcrEngine("Texto detectado por OCR")));
        ReadableDocument document = document(
                block("b1", "Pagina visual sin texto narrable", 1, 1));

        PdfResolvedTextLayerProjection projection = useCase.resolve(new PdfResolvedTextLayerRequest(
                document, List.of(1), PdfTextResolutionPolicy.OCR_WHEN_UNAVAILABLE, null, 144, "spa+eng"));

        assertEquals(List.of(1), projection.ocrPagesAttempted());
        assertEquals(PdfTextLayerOrigin.OCR_LOCAL, projection.layerForPage(1).origin());
        assertEquals("Texto detectado por OCR", projection.layerForPage(1).lines().getFirst().text());
    }

    @Test
    void searchIgnoresNativeTextEvenWhenBboxMatches() {
        BuildPdfResolvedTextLayerUseCase resolver = new BuildPdfResolvedTextLayerUseCase(
                new BuildPdfNativeTextLayerUseCase(),
                new BuildPdfOcrTextLayerUseCase(new FakeOcrEngine("")));
        SearchPdfTextUseCase search = new SearchPdfTextUseCase(resolver);
        ReadableDocument document = document(blockWithBbox("b1", "Review of Calculus", 2, 10));

        PdfTextSearchProjection projection = search.search(new PdfTextSearchRequest(
                document, "calculus", false, 20, 5, null));

        assertEquals(0, projection.results().size());
    }

    @Test
    void searchCanUseOcrForUnavailablePagesWhenExplicitlyRequested() {
        BuildPdfResolvedTextLayerUseCase resolver = new BuildPdfResolvedTextLayerUseCase(
                new BuildPdfNativeTextLayerUseCase(),
                new BuildPdfOcrTextLayerUseCase(new FakeOcrEngine("Population growth model")));
        SearchPdfTextUseCase search = new SearchPdfTextUseCase(resolver);
        ReadableDocument document = document(block("b1", "visual page", 1, 1));

        PdfTextSearchProjection projection = search.search(new PdfTextSearchRequest(
                document, "growth", true, 20, 5, null));

        assertEquals(1, projection.results().size());
        assertEquals("", projection.results().getFirst().blockId());
        assertEquals(PdfTextLayerOrigin.OCR_LOCAL, projection.results().getFirst().origin());
        assertFalse(projection.results().getFirst().toHighlight().text().isBlank());
    }

    @Test
    void searchMapsOcrLineInsidePersistedParagraphBackToBlockId() {
        BuildPdfResolvedTextLayerUseCase resolver = new BuildPdfResolvedTextLayerUseCase(
                new BuildPdfNativeTextLayerUseCase(),
                new BuildPdfOcrTextLayerUseCase(new FakeOcrEngine("")));
        SearchPdfTextUseCase search = new SearchPdfTextUseCase(resolver);
        ReadableDocument document = document(DocumentBlock.of("ocr1", DocumentBlockType.PARAGRAPH,
                "First OCR line. Population growth model continues here.", "", Map.of(
                "sourcePage", "1",
                "sourcePageCount", "1",
                "bboxUnits", "pdf-points",
                "bbox", "10.000,20.000,500.000,90.000",
                "pageWidth", "612",
                "pageHeight", "792",
                "ocr", "true",
                "nativeText", "false",
                "extractionMode", "ocr-local")));

        PdfTextSearchProjection projection = search.search(new PdfTextSearchRequest(
                document, "growth model", false, 20, 5, null));

        assertEquals(1, projection.results().size());
        assertEquals("ocr1", projection.results().getFirst().blockId());
        assertEquals(PdfTextLayerOrigin.OCR_LOCAL, projection.results().getFirst().origin());
    }

    @Test
    void visualReadingProjectionUsesOcrOriginForImportedOcrBlocksWithBbox() {
        ReadableDocument document = document(DocumentBlock.of("ocr1", DocumentBlockType.PARAGRAPH,
                "Texto detectado por OCR para lectura sincronizada.", "", Map.of(
                "sourcePage", "1",
                "sourcePageCount", "1",
                "bboxUnits", "pdf-points",
                "bbox", "72.000,90.000,520.000,116.000",
                "pageWidth", "612",
                "pageHeight", "792",
                "ocr", "true",
                "nativeText", "false",
                "extractionMode", "ocr-local")));

        PdfVisualReadingProjection projection = new BuildPdfVisualReadingProjectionUseCase(
                new BuildPdfNativeTextLayerUseCase()).build(document);
        PdfVisualTextHighlight highlight = projection.highlightForBlock("ocr1").orElseThrow();

        assertEquals(PdfTextLayerOrigin.OCR_LOCAL, highlight.origin());
        assertEquals(1, highlight.pageNumber());
        assertEquals("Texto detectado por OCR para lectura sincronizada.", highlight.text());
    }

    @Test
    void enhancedOutlineUsesOcrContentsButKeepsOriginalBlockAnchors() {
        BuildDocumentOutlineUseCase base = new BuildDocumentOutlineUseCase();
        BuildPdfResolvedTextLayerUseCase resolver = new BuildPdfResolvedTextLayerUseCase(
                new BuildPdfNativeTextLayerUseCase(),
                new BuildPdfOcrTextLayerUseCase(new FakeOcrEngine(String.join("\n",
                        "Table of Contents",
                        "1.1 Review of Calculus 4",
                        "1.2 Round-off Errors 8",
                        "1.3 Algorithms 12",
                        "2.1 Equations 20"))));
        BuildPdfEnhancedOutlineUseCase enhanced = new BuildPdfEnhancedOutlineUseCase(base, resolver);
        ReadableDocument document = document(
                block("cover", "visual contents page", 1, 30),
                block("b4", "body page four", 4, 30),
                block("b8", "body page eight", 8, 30),
                block("b12", "body page twelve", 12, 30),
                block("b20", "body page twenty", 20, 30));

        DocumentOutlineProjection projection = enhanced.build(document, null);

        assertEquals(DocumentOutlineOrigin.CONTENTS, projection.origin());
        assertEquals(4, projection.indexedEntryCount());
        assertEquals("b4", projection.entries().getFirst().blockId());
    }

    private static ReadableDocument document(DocumentBlock... blocks) {
        return new ReadableDocument("sample", SourceDocumentFormat.PDF, Path.of("sample.pdf"), List.of(blocks));
    }

    private static DocumentBlock block(String id, String text, int sourcePage, int sourcePageCount) {
        return DocumentBlock.of(id, DocumentBlockType.PARAGRAPH, text, "", Map.of(
                "sourcePage", Integer.toString(sourcePage),
                "sourcePageCount", Integer.toString(sourcePageCount),
                "visualBlock", "true",
                "extractionMode", "visual-fallback"));
    }

    private static DocumentBlock blockWithBbox(String id, String text, int sourcePage, int sourcePageCount) {
        return DocumentBlock.of(id, DocumentBlockType.PARAGRAPH, text, "", Map.of(
                "sourcePage", Integer.toString(sourcePage),
                "sourcePageCount", Integer.toString(sourcePageCount),
                "bboxUnits", "pdf-points",
                "bbox", "10.000,20.000,300.000,42.000",
                "pageWidth", "612",
                "pageHeight", "792"));
    }

    private static final class FakeOcrEngine implements PdfOcrEngine {
        private final String text;

        private FakeOcrEngine(String text) {
            this.text = text;
        }

        @Override
        public PdfOcrPageResult recognize(PdfOcrRequest request) {
            List<PdfOcrLine> lines = new java.util.ArrayList<>();
            int y = 20;
            int ordinal = 0;
            for (String raw : text.split("\\R")) {
                if (raw.isBlank()) {
                    continue;
                }
                PdfPageRegion region = new PdfPageRegion(request.pageNumber(), 10, y, 500, y + 18, 612, 792);
                PdfOcrWord word = new PdfOcrWord(raw, region, 0.91);
                lines.add(new PdfOcrLine(request.pageNumber(), raw, region, List.of(word), 0.91));
                y += 22;
                ordinal++;
            }
            PdfTextLayer layer = lines.isEmpty()
                    ? new PdfTextLayer(request.pageNumber(), PdfTextLayerOrigin.UNAVAILABLE, List.of(), List.of())
                    : new PdfTextLayer(request.pageNumber(), PdfTextLayerOrigin.OCR_LOCAL,
                    lines.stream().map(PdfOcrLine::toTextLine).toList(), List.of());
            return new PdfOcrPageResult(request.pageNumber(), request.dpi(), 1200, 1600, 612, 792,
                    lines, lines.stream().flatMap(line -> line.words().stream()).toList(), layer, List.of());
        }
    }
}

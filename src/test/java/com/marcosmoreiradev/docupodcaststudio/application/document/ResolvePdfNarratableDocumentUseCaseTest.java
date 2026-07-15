package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.application.script.BuildNarrationScriptUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlockType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentFormat;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.ReadingProfile;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ResolvePdfNarratableDocumentUseCaseTest {
    @Test
    void imageOnlyPdfProducesPersistentNarratableOcrBlocks() {
        ResolvePdfNarratableDocumentUseCase useCase = new ResolvePdfNarratableDocumentUseCase(
                new BuildPdfNativeTextLayerUseCase(),
                new BuildPdfOcrTextLayerUseCase(new FakeOcrEngine(List.of("Using this estimate produces the error estimation."))),
                null);
        ReadableDocument visualOnly = document(visualFallback("page1", 1, 1));

        PdfNarratableDocumentResolution resolution = useCase.resolve(new PdfNarratableDocumentRequest(
                visualOnly, null, false, 10));

        assertTrue(resolution.changed());
        assertEquals(List.of(1), resolution.pagesProcessed());
        assertTrue(resolution.document().wordCount() > 0);
        assertEquals(1, resolution.document().narratableBlockCount());
        DocumentBlock block = resolution.document().blocks().getFirst();
        assertEquals("true", block.metadata().get("ocr"));
        assertEquals("false", block.metadata().get("nativeText"));
        assertEquals("1", block.metadata().get("sourcePage"));
        assertEquals("pdf-points", block.metadata().get("bboxUnits"));
        assertEquals("1", block.metadata().get("pdfTextMapVersion"));
        assertTrue(block.metadata().containsKey("lineBboxes"));
        assertTrue(block.metadata().containsKey("lineCharRanges"));
        assertEquals("ocr-local", block.metadata().get("extractionMode"));
        assertFalse(resolution.document().issues().stream().anyMatch(issue -> issue.code().equals("pdf-ocr-pending")));
    }

    @Test
    void nativePdfWithBboxIsReplacedByOcrBlocks() {
        ResolvePdfNarratableDocumentUseCase useCase = new ResolvePdfNarratableDocumentUseCase(
                new BuildPdfNativeTextLayerUseCase(),
                new BuildPdfOcrTextLayerUseCase(new FakeOcrEngine(List.of("OCR text replaces native text"))),
                null);
        ReadableDocument nativePdf = document(nativeBlock("b1", "Texto nativo suficiente", 1, 1));

        PdfNarratableDocumentResolution resolution = useCase.resolve(new PdfNarratableDocumentRequest(
                nativePdf, null, false, 10));

        assertTrue(resolution.changed());
        assertEquals(List.of(1), resolution.pagesProcessed());
        assertTrue(resolution.document().blocks().stream().noneMatch(block -> "true".equals(block.metadata().get("nativeText"))));
        assertTrue(resolution.document().blocks().stream().anyMatch(block -> "true".equals(block.metadata().get("ocr"))));
    }

    @Test
    void ocrFailureKeepsVisualFallbackAndWarning() {
        ResolvePdfNarratableDocumentUseCase useCase = new ResolvePdfNarratableDocumentUseCase(
                new BuildPdfNativeTextLayerUseCase(),
                new BuildPdfOcrTextLayerUseCase(request -> {
                    throw new PdfOcrException(PdfOcrErrorCode.TESSERACT_NOT_FOUND, "Tesseract missing");
                }),
                null);
        ReadableDocument visualOnly = document(visualFallback("page1", 1, 1));

        PdfNarratableDocumentResolution resolution = useCase.resolve(new PdfNarratableDocumentRequest(
                visualOnly, null, false, 10));

        assertTrue(resolution.changed());
        assertEquals(0, resolution.document().narratableBlockCount());
        assertEquals(DocumentBlockType.IMAGE_NOTICE, resolution.document().blocks().getFirst().type());
        assertTrue(resolution.document().issues().stream().anyMatch(issue -> issue.code().equals("pdf-ocr-runtime-missing")));
    }

    @Test
    void ocrTextDiscardedByClassifierGetsSpecificWarning() {
        ResolvePdfNarratableDocumentUseCase useCase = new ResolvePdfNarratableDocumentUseCase(
                new BuildPdfNativeTextLayerUseCase(),
                new BuildPdfOcrTextLayerUseCase(new FakeOcrEngine(List.of("1.57079633 2.09439511 1.99857073 2.00000555"))),
                null);
        ReadableDocument visualOnly = document(visualFallback("page1", 1, 1));

        PdfNarratableDocumentResolution resolution = useCase.resolve(new PdfNarratableDocumentRequest(
                visualOnly, null, false, 10));

        assertTrue(resolution.changed());
        assertEquals(0, resolution.document().narratableBlockCount());
        assertTrue(resolution.document().issues().stream().anyMatch(issue -> issue.code().equals("pdf-ocr-text-discarded")));
        assertFalse(resolution.document().issues().stream().anyMatch(issue -> issue.code().equals("pdf-ocr-empty")));
    }

    @Test
    void mixedMathTablePageKeepsNarratableProseLinesBeforeGrouping() {
        ResolvePdfNarratableDocumentUseCase useCase = new ResolvePdfNarratableDocumentUseCase(
                new BuildPdfNativeTextLayerUseCase(),
                new BuildPdfOcrTextLayerUseCase(new FakeOcrEngine(List.of(
                        "R5,5 = R5,4 + 1/255 (R5,4 - R4,4) = 1.99999999",
                        "These results are shown in Table 4.9.",
                        "1.57079633 2.09439511 1.99857073 2.00000555",
                        "Nesting has reduced the relative error for the chopping approximation to less than 10%",
                        "Polynomials should always be expressed in nested form before performing an evaluation."))),
                null);
        ReadableDocument visualOnly = document(visualFallback("page1", 1, 1));

        PdfNarratableDocumentResolution resolution = useCase.resolve(new PdfNarratableDocumentRequest(
                visualOnly, null, false, 10));

        String text = resolution.document().blocks().stream()
                .filter(DocumentBlock::narratable)
                .map(DocumentBlock::text)
                .collect(java.util.stream.Collectors.joining(" "));
        assertTrue(resolution.document().narratableBlockCount() > 0);
        assertTrue(text.contains("These results are shown in Table 4.9."));
        assertTrue(text.contains("Nesting has reduced the relative error"));
        assertTrue(text.contains("Polynomials should always be expressed"));
        assertFalse(text.contains("R5,5 = R5,4"));
        assertFalse(text.contains("1.57079633 2.09439511"));
    }

    @Test
    void ocrGroupingSeparatesNearbySideNotesByColorAndColumn() {
        ResolvePdfNarratableDocumentUseCase useCase = new ResolvePdfNarratableDocumentUseCase(
                new BuildPdfNativeTextLayerUseCase(),
                new BuildPdfOcrTextLayerUseCase(new PositionedOcrEngine(List.of(
                        positionedLine("The improved accuracy of Simpson's rule is intuitively explained by the fact that it provides better balance.",
                                54, 130, 180, 184, "blue"),
                        positionedLine("Definition 4.1 implies that the Trapezoidal and Simpson's rules have degrees of precision one and three, respectively.",
                                230, 132, 560, 150, "black"),
                        positionedLine("Integration and summation are linear operations; that is,",
                                230, 154, 560, 172, "black")))),
                null);

        PdfNarratableDocumentResolution resolution = useCase.resolve(new PdfNarratableDocumentRequest(
                document(visualFallback("page1", 1, 1)), null, false, 10));

        List<String> blocks = resolution.document().blocks().stream()
                .filter(DocumentBlock::narratable)
                .map(DocumentBlock::text)
                .toList();
        assertEquals(2, blocks.size());
        assertTrue(blocks.stream().anyMatch(text -> text.contains("The improved accuracy of Simpson's rule")));
        assertTrue(blocks.stream().anyMatch(text -> text.contains("Definition 4.1 implies")
                && text.contains("Integration and summation are linear operations")));
        assertFalse(blocks.stream().anyMatch(text -> text.contains("The improved accuracy")
                && text.contains("Definition 4.1 implies")));
    }

    @Test
    void ocrBlocksFeedNarrationSegmentsWithSourceBlockIds() {
        ResolvePdfNarratableDocumentUseCase useCase = new ResolvePdfNarratableDocumentUseCase(
                new BuildPdfNativeTextLayerUseCase(),
                new BuildPdfOcrTextLayerUseCase(new FakeOcrEngine(List.of("Romberg integration improves estimates."))),
                null);
        ReadableDocument resolved = useCase.resolve(new PdfNarratableDocumentRequest(
                document(visualFallback("page1", 1, 1)), null, false, 10)).document();

        var script = new BuildNarrationScriptUseCase().build(resolved, "es", false, ReadingProfile.academicDefaults());

        assertEquals(1, script.segmentCount());
        assertEquals(resolved.blocks().getFirst().id(), script.segments().getFirst().sourceBlockIds().getFirst());
    }

    @Test
    void pageScopedRequestProcessesOnlyRequestedPages() {
        ResolvePdfNarratableDocumentUseCase useCase = new ResolvePdfNarratableDocumentUseCase(
                new BuildPdfNativeTextLayerUseCase(),
                new BuildPdfOcrTextLayerUseCase(new FakeOcrEngine(List.of("Newton method converges near a simple root."))),
                null);
        ReadableDocument visualOnly = document(
                visualFallback("page1", 1, 3),
                visualFallback("page2", 2, 3),
                visualFallback("page3", 3, 3));

        PdfNarratableDocumentResolution resolution = useCase.resolve(new PdfNarratableDocumentRequest(
                visualOnly, null, false, 10, List.of(3)));

        assertTrue(resolution.changed());
        assertEquals(List.of(3), resolution.pagesProcessed());
        assertTrue(resolution.document().blocks().stream().anyMatch(block ->
                block.narratable() && "3".equals(block.metadata().get("sourcePage"))));
        assertTrue(resolution.document().blocks().stream().anyMatch(block ->
                "visual-fallback".equals(block.metadata().get("extractionMode"))
                        && "1".equals(block.metadata().get("sourcePage"))));
        assertTrue(resolution.document().blocks().stream().anyMatch(block ->
                "visual-fallback".equals(block.metadata().get("extractionMode"))
                        && "2".equals(block.metadata().get("sourcePage"))));
    }

    @Test
    void usesConfiguredOcrLanguagesDpiAndCachePolicyWhenResolvingPages() {
        AtomicReference<PdfOcrRequest> captured = new AtomicReference<>();
        ResolvePdfNarratableDocumentUseCase useCase = new ResolvePdfNarratableDocumentUseCase(
                new BuildPdfNativeTextLayerUseCase(),
                new BuildPdfOcrTextLayerUseCase(request -> {
                    captured.set(request);
                    return new FakeOcrEngine(List.of("Configurable OCR language selection works.")).recognize(request);
                }),
                null,
                () -> new OperationalSettings.OcrSettings("managed-local", "", "eng+spa", 420, 180, false, ""));

        useCase.resolve(new PdfNarratableDocumentRequest(
                document(visualFallback("page1", 1, 1)), Path.of("cache"), false, 10));

        assertEquals("eng+spa", captured.get().languages());
        assertEquals(420, captured.get().dpi());
        assertNull(captured.get().cacheDirectory());
    }

    private static ReadableDocument document(DocumentBlock... blocks) {
        return new ReadableDocument("sample", SourceDocumentFormat.PDF, Path.of("sample.pdf"), List.of(blocks));
    }

    private static DocumentBlock visualFallback(String id, int page, int pageCount) {
        return DocumentBlock.of(id, DocumentBlockType.IMAGE_NOTICE, "Pagina visual sin texto nativo", "", Map.of(
                "sourcePage", Integer.toString(page),
                "sourcePageCount", Integer.toString(pageCount),
                "extractionMode", "visual-fallback",
                "visualBlock", "true",
                "visualRenderAvailable", "true"));
    }

    private static DocumentBlock nativeBlock(String id, String text, int page, int pageCount) {
        return DocumentBlock.of(id, DocumentBlockType.PARAGRAPH, text, "", Map.of(
                "sourcePage", Integer.toString(page),
                "sourcePageCount", Integer.toString(pageCount),
                "bboxUnits", "pdf-points",
                "bbox", "72.000,90.000,520.000,116.000",
                "pageWidth", "612",
                "pageHeight", "792",
                "nativeText", "true",
                "extractionMode", "pdfbox-text"));
    }

    private static final class FakeOcrEngine implements PdfOcrEngine {
        private final List<String> lines;

        private FakeOcrEngine(List<String> lines) {
            this.lines = lines;
        }

        @Override
        public PdfOcrPageResult recognize(PdfOcrRequest request) {
            List<PdfOcrLine> ocrLines = new java.util.ArrayList<>();
            int y = 72;
            for (String line : lines) {
                PdfPageRegion region = new PdfPageRegion(request.pageNumber(), 72, y, 520, y + 18, 612, 792);
                PdfOcrWord word = new PdfOcrWord(line, region, 0.93);
                ocrLines.add(new PdfOcrLine(request.pageNumber(), line, region, List.of(word), 0.93));
                y += 24;
            }
            PdfTextLayer layer = new PdfTextLayer(request.pageNumber(), PdfTextLayerOrigin.OCR_LOCAL,
                    ocrLines.stream().map(PdfOcrLine::toTextLine).toList(), List.of());
            return new PdfOcrPageResult(request.pageNumber(), request.dpi(), 1200, 1600, 612, 792,
                    ocrLines, ocrLines.stream().flatMap(line -> line.words().stream()).toList(), layer, List.of());
        }
    }

    private static PositionedLine positionedLine(String text,
                                                 double xMin,
                                                 double yMin,
                                                 double xMax,
                                                 double yMax,
                                                 String dominantColor) {
        return new PositionedLine(text, xMin, yMin, xMax, yMax, dominantColor);
    }

    private record PositionedLine(String text,
                                  double xMin,
                                  double yMin,
                                  double xMax,
                                  double yMax,
                                  String dominantColor) {
    }

    private static final class PositionedOcrEngine implements PdfOcrEngine {
        private final List<PositionedLine> lines;

        private PositionedOcrEngine(List<PositionedLine> lines) {
            this.lines = lines;
        }

        @Override
        public PdfOcrPageResult recognize(PdfOcrRequest request) {
            List<PdfOcrLine> ocrLines = new java.util.ArrayList<>();
            for (PositionedLine line : lines) {
                PdfPageRegion region = new PdfPageRegion(
                        request.pageNumber(), line.xMin(), line.yMin(), line.xMax(), line.yMax(), 612, 792);
                PdfOcrWord word = new PdfOcrWord(line.text(), region, 0.93);
                ocrLines.add(new PdfOcrLine(
                        request.pageNumber(), line.text(), region, List.of(word), 0.93, line.dominantColor()));
            }
            PdfTextLayer layer = new PdfTextLayer(request.pageNumber(), PdfTextLayerOrigin.OCR_LOCAL,
                    ocrLines.stream().map(PdfOcrLine::toTextLine).toList(), List.of());
            return new PdfOcrPageResult(request.pageNumber(), request.dpi(), 1200, 1600, 612, 792,
                    ocrLines, ocrLines.stream().flatMap(line -> line.words().stream()).toList(), layer, List.of());
        }
    }
}

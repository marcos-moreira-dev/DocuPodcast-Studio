package com.marcosmoreiradev.docupodcaststudio.application.document;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PreparePdfPageNativeFirstTest {
    @TempDir Path temp;

    @Test
    void reliableDigitalPageDoesNotStartOcr() {
        AtomicInteger ocrCalls = new AtomicInteger();
        BuildPdfOcrTextLayerUseCase ocr = new BuildPdfOcrTextLayerUseCase(request -> {
            ocrCalls.incrementAndGet();
            throw new AssertionError("Tesseract must not run for reliable native text.");
        });
        PdfNativePageExtractor nativeExtractor = (source, page) -> {
            PdfPageRegion box = new PdfPageRegion(page, 20, 30, 580, 70, 612, 792);
            return new PdfTextLayer(page, PdfTextLayerOrigin.NATIVE_BBOX,
                    List.of(new PdfTextLine(page,
                            "Este párrafo digital contiene texto nativo continuo, legible y suficientemente extenso.",
                            box, List.of(), 1.0)), List.of());
        };
        InMemoryPreparedPdfDocumentRepository repository =
                new InMemoryPreparedPdfDocumentRepository();
        PreparedPdfWorkspaceRef workspace =
                PreparedPdfTestFixtures.workspace(repository, temp, 1);
        PreparePdfPageUseCase useCase = new PreparePdfPageUseCase(
                ocr, null, null, repository, nativeExtractor);

        PreparePdfPageResult result = useCase.execute(new PreparePdfPageRequest(
                workspace, null, 1, false, null));

        assertTrue(result.succeeded());
        assertEquals(0, ocrCalls.get());
        assertTrue(result.preparedPage().preparationMetrics().nativeExtractionUsed());
        assertTrue(!result.preparedPage().preparationMetrics().ocrUsed());
        assertEquals("Este párrafo digital contiene texto nativo continuo, legible y suficientemente extenso.",
                result.preparedPage().regions().getFirst().effectiveText());
    }

    @Test
    void forcedOcrOverridesReliableNativeText() {
        AtomicInteger ocrCalls = new AtomicInteger();
        BuildPdfOcrTextLayerUseCase ocr = new BuildPdfOcrTextLayerUseCase(request -> {
            ocrCalls.incrementAndGet();
            PdfPageRegion box = new PdfPageRegion(1, 20, 30, 580, 70, 612, 792);
            PdfOcrWord word = new PdfOcrWord(
                    "Texto recuperado mediante OCR forzado.", box, 0.96);
            PdfOcrLine line = new PdfOcrLine(
                    1, word.text(), box, List.of(word), word.confidence());
            PdfTextLayer layer = new PdfTextLayer(
                    1, PdfTextLayerOrigin.OCR_LOCAL,
                    List.of(line.toTextLine()), List.of());
            return new PdfOcrPageResult(
                    1, 216, 1836, 2376, 612, 792,
                    List.of(line), List.of(word), layer, List.of());
        });
        PdfNativePageExtractor nativeExtractor = (source, page) -> {
            PdfPageRegion box = new PdfPageRegion(page, 20, 30, 580, 70, 612, 792);
            return new PdfTextLayer(page, PdfTextLayerOrigin.NATIVE_BBOX,
                    List.of(new PdfTextLine(page,
                            "Este párrafo digital contiene texto nativo continuo, legible y suficientemente extenso.",
                            box, List.of(), 1.0)), List.of());
        };
        InMemoryPreparedPdfDocumentRepository repository =
                new InMemoryPreparedPdfDocumentRepository();
        PreparedPdfWorkspaceRef workspace =
                PreparedPdfTestFixtures.workspace(repository, temp, 1);
        PreparePdfPageUseCase useCase = new PreparePdfPageUseCase(
                ocr, null, null, repository, nativeExtractor);

        PreparePdfPageResult result = useCase.execute(new PreparePdfPageRequest(
                workspace, null, 1, true, null));

        assertTrue(result.succeeded());
        assertEquals(1, ocrCalls.get());
        assertTrue(result.preparedPage().preparationMetrics().nativeExtractionUsed());
        assertTrue(result.preparedPage().preparationMetrics().ocrUsed());
        assertEquals("Texto recuperado mediante OCR forzado.",
                result.preparedPage().regions().getFirst().effectiveText());
    }

    @Test
    void semanticRejectionUsesUserFacingCopyWithoutThresholdDetails() {
        PdfSemanticCoverageException rejection = new PdfSemanticCoverageException(
                "Coverage 0.81 < 0.84",
                new PdfSemanticCoverageResult(PdfSemanticCoverageStatus.REJECTED,
                        true, 0.81, List.of("nativeTextCoverageGap"), List.of()));

        String message = PreparePdfPageUseCase.semanticFailureMessage(rejection, 3);

        assertEquals("PDF p. 3: esta página no pudo interpretarse con suficiente fiabilidad.",
                message);
        assertTrue(!message.contains("0.81"));
        assertTrue(!message.contains("Coverage"));
    }
}

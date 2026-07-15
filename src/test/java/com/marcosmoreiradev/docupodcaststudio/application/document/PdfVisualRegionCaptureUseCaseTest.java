package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.study.StudySourceReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PdfVisualRegionCaptureUseCaseTest {
    @TempDir
    Path tempDir;

    @Test
    void viewportSelectionConvertsToPdfPointsAndWritesCropPng() throws Exception {
        RecordingRenderEngine engine = new RecordingRenderEngine();
        CapturePdfVisualRegionUseCase useCase = new CapturePdfVisualRegionUseCase(engine);
        Path target = tempDir.resolve("capture.png");

        PdfRegionCaptureResult result = useCase.capture(new PdfRegionCaptureRequest(
                tempDir.resolve("book.pdf"),
                new PdfViewportSelection(3, 100, 200, 300, 500, 1224, 1584, 612, 792),
                target,
                6,
                144,
                8_000_000,
                Color.WHITE,
                true));

        assertEquals(3, engine.lastRequest.pageNumber());
        assertEquals(50.0, engine.lastRequest.xMinPoints(), 0.001);
        assertEquals(100.0, engine.lastRequest.yMinPoints(), 0.001);
        assertEquals(150.0, engine.lastRequest.xMaxPoints(), 0.001);
        assertEquals(250.0, engine.lastRequest.yMaxPoints(), 0.001);
        assertEquals(6.0, engine.lastRequest.paddingPoints(), 0.001);
        assertEquals("50.000,100.000,150.000,250.000", result.bbox());
        assertEquals(144, result.dpi());
        assertEquals(32, result.widthPixels());
        assertEquals(24, result.heightPixels());
        assertTrue(Files.isRegularFile(target));
    }

    @Test
    void studySourceReferenceCanRepresentVisualPdfCaptureWithoutText() {
        StudySourceReference source = StudySourceReference.visualRegion(
                "pdf-region-0001",
                "",
                "12",
                "10.000,20.000,130.000,220.000",
                "STUDY-CROP-1-1");

        assertEquals("pdf-region-0001", source.blockId());
        assertEquals("", source.selectedText());
        assertEquals("12", source.sourcePage());
        assertEquals("10.000,20.000,130.000,220.000", source.bbox());
        assertEquals("STUDY-CROP-1-1", source.sourceCropAssetId());
    }

    private static final class RecordingRenderEngine implements PdfRenderEngine {
        private PdfCropRenderRequest lastRequest;

        @Override
        public PdfDocumentInfo inspect(Path sourcePdf, PdfOpenOptions options) {
            throw new UnsupportedOperationException("not needed");
        }

        @Override
        public PdfPageRenderResult renderPage(PdfPageRenderRequest request) {
            throw new UnsupportedOperationException("not needed");
        }

        @Override
        public PdfPageRenderResult renderCrop(PdfCropRenderRequest request) {
            lastRequest = request;
            BufferedImage image = new BufferedImage(32, 24, BufferedImage.TYPE_INT_RGB);
            return new PdfPageRenderResult(
                    request.pageNumber(),
                    request.pageNumber(),
                    612,
                    792,
                    request.dpi(),
                    image,
                    "fake-crop",
                    List.of());
        }
    }
}

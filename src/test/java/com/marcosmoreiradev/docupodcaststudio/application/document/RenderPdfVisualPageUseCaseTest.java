package com.marcosmoreiradev.docupodcaststudio.application.document;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class RenderPdfVisualPageUseCaseTest {
    @Test
    void delegatesPageRenderToEngine() throws Exception {
        RecordingPdfRenderEngine engine = new RecordingPdfRenderEngine();
        RenderPdfVisualPageUseCase useCase = new RenderPdfVisualPageUseCase(engine);
        PdfPageRenderRequest request = new PdfPageRenderRequest(
                Path.of("book.pdf"), 2, 144, 1_000_000, Color.WHITE, true);

        PdfPageRenderResult result = useCase.render(request);

        assertSame(request, engine.lastRequest);
        assertEquals(2, result.pageNumber());
        assertEquals(3, result.pageCount());
        assertEquals("pdfbox", result.renderMode());
    }

    private static final class RecordingPdfRenderEngine implements PdfRenderEngine {
        private PdfPageRenderRequest lastRequest;

        @Override
        public PdfDocumentInfo inspect(Path sourcePdf, PdfOpenOptions options) {
            return new PdfDocumentInfo(sourcePdf, 3, false, true, "",
                    List.of(new PdfPageInfo(1, 612, 792, 0)), List.of());
        }

        @Override
        public PdfPageRenderResult renderPage(PdfPageRenderRequest request) {
            lastRequest = request;
            return new PdfPageRenderResult(
                    request.pageNumber(),
                    3,
                    612,
                    792,
                    request.dpi(),
                    new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB),
                    "pdfbox",
                    List.of());
        }

        @Override
        public PdfPageRenderResult renderCrop(PdfCropRenderRequest request) {
            throw new UnsupportedOperationException("No usado en esta prueba");
        }
    }
}

package com.marcosmoreiradev.docupodcaststudio.application.document;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class BuildPdfVisualDocumentUseCaseTest {
    @Test
    void exposesRotatedPagesInDisplayedCoordinateSpace() throws Exception {
        Path source = Path.of("rotated.pdf");
        PdfRenderEngine engine = new PdfRenderEngine() {
            @Override
            public PdfDocumentInfo inspect(Path ignored, PdfOpenOptions options) {
                return new PdfDocumentInfo(source, 2, false, true, "", List.of(
                        new PdfPageInfo(1, 600, 800, 0),
                        new PdfPageInfo(2, 600, 800, 90)), List.of());
            }

            @Override
            public PdfPageRenderResult renderPage(PdfPageRenderRequest request) {
                throw new UnsupportedOperationException();
            }

            @Override
            public PdfPageRenderResult renderCrop(PdfCropRenderRequest request) {
                throw new UnsupportedOperationException();
            }
        };

        PdfVisualDocument visual = new BuildPdfVisualDocumentUseCase(engine).build(source, 144);

        assertEquals(600, visual.pages().getFirst().widthPoints());
        assertEquals(800, visual.pages().getFirst().heightPoints());
        assertEquals(0, visual.pages().getFirst().rotationDegrees());
        assertEquals(800, visual.pages().getLast().widthPoints());
        assertEquals(600, visual.pages().getLast().heightPoints());
        assertEquals(90, visual.pages().getLast().rotationDegrees());
    }
}

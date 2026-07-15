package com.marcosmoreiradev.docupodcaststudio.application.document;

import java.util.Objects;

/** Runs local OCR for one PDF page and returns the internal text layer used by reading/voice flows. */
public final class BuildPdfOcrTextLayerUseCase {
    private final PdfOcrEngine ocrEngine;

    public BuildPdfOcrTextLayerUseCase(PdfOcrEngine ocrEngine) {
        this.ocrEngine = Objects.requireNonNull(ocrEngine, "ocrEngine");
    }

    public PdfOcrPageResult recognize(PdfOcrRequest request) throws PdfOcrException {
        return ocrEngine.recognize(request);
    }
}

package com.marcosmoreiradev.docupodcaststudio.application.document;

import java.util.List;

/** OCR output for one PDF page. */
public record PdfOcrPageResult(
        int pageNumber,
        int dpi,
        int imageWidthPixels,
        int imageHeightPixels,
        double pageWidthPoints,
        double pageHeightPoints,
        List<PdfOcrLine> lines,
        List<PdfOcrWord> words,
        PdfTextLayer textLayer,
        List<String> warnings
) {
    public PdfOcrPageResult {
        pageNumber = Math.max(0, pageNumber);
        dpi = Math.max(1, dpi);
        imageWidthPixels = Math.max(0, imageWidthPixels);
        imageHeightPixels = Math.max(0, imageHeightPixels);
        lines = lines == null ? List.of() : List.copyOf(lines);
        words = words == null ? List.of() : List.copyOf(words);
        textLayer = textLayer == null
                ? new PdfTextLayer(pageNumber, PdfTextLayerOrigin.UNAVAILABLE, List.of(), List.of("OCR sin capa textual."))
                : textLayer;
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }
}

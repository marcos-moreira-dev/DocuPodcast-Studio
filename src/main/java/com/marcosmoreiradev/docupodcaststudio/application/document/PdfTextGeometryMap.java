package com.marcosmoreiradev.docupodcaststudio.application.document;

import java.util.List;

/**
 * Raster-derived geometric evidence for one page. It is deliberately not a
 * semantic document model: Qwen remains authoritative for region type,
 * reading order and narration while this map supplies physical text boxes.
 */
public record PdfTextGeometryMap(
        int pageNumber,
        PdfTextLayer textLayer,
        List<PdfOcrWord> words,
        String origin,
        List<String> warnings
) {
    public PdfTextGeometryMap {
        pageNumber = Math.max(0, pageNumber);
        textLayer = textLayer == null
                ? new PdfTextLayer(pageNumber, PdfTextLayerOrigin.UNAVAILABLE,
                List.of(), List.of("OCR geometry unavailable."))
                : textLayer;
        words = words == null ? List.of() : List.copyOf(words);
        origin = origin == null ? "UNAVAILABLE" : origin.strip();
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }

    public boolean available() {
        return textLayer.available();
    }

    public static PdfTextGeometryMap unavailable(int pageNumber, String warning) {
        List<String> warnings = warning == null || warning.isBlank()
                ? List.of() : List.of(warning);
        return new PdfTextGeometryMap(pageNumber,
                new PdfTextLayer(pageNumber, PdfTextLayerOrigin.UNAVAILABLE,
                        List.of(), warnings),
                List.of(), "UNAVAILABLE", warnings);
    }
}

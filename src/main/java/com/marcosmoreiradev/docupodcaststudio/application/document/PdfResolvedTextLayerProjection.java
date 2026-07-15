package com.marcosmoreiradev.docupodcaststudio.application.document;

import java.util.List;

/** Resolved PDF text layers plus diagnostics for native/OCR composition. */
public record PdfResolvedTextLayerProjection(
        List<PdfTextLayer> layers,
        List<Integer> ocrPagesAttempted,
        List<String> warnings
) {
    public PdfResolvedTextLayerProjection {
        layers = layers == null ? List.of() : List.copyOf(layers);
        ocrPagesAttempted = ocrPagesAttempted == null ? List.of() : List.copyOf(ocrPagesAttempted);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }

    public PdfTextLayer layerForPage(int pageNumber) {
        return layers.stream()
                .filter(layer -> layer.pageNumber() == pageNumber)
                .findFirst()
                .orElse(new PdfTextLayer(pageNumber, PdfTextLayerOrigin.UNAVAILABLE, List.of(),
                        List.of("Pagina sin capa textual resuelta.")));
    }
}

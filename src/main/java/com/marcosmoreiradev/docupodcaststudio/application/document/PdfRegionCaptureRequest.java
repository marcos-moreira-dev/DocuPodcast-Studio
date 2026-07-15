package com.marcosmoreiradev.docupodcaststudio.application.document;

import java.awt.Color;
import java.nio.file.Path;

/** Request to materialize a user-selected PDF viewport region as a PNG crop. */
public record PdfRegionCaptureRequest(
        Path sourcePath,
        PdfViewportSelection selection,
        Path targetPng,
        double paddingPoints,
        int dpi,
        long maxPixelCount,
        Color backgroundColor,
        boolean renderAnnotations
) {
    public PdfRegionCaptureRequest {
        if (sourcePath == null) {
            throw new IllegalArgumentException("sourcePath is required");
        }
        if (selection == null) {
            throw new IllegalArgumentException("selection is required");
        }
        paddingPoints = Math.max(0.0, paddingPoints);
        dpi = dpi <= 0 ? PdfPageRenderRequest.DEFAULT_DPI : dpi;
        maxPixelCount = maxPixelCount <= 0 ? PdfPageRenderRequest.DEFAULT_MAX_PIXEL_COUNT : maxPixelCount;
        backgroundColor = backgroundColor == null ? Color.WHITE : backgroundColor;
    }
}

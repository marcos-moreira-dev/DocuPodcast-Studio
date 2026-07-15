package com.marcosmoreiradev.docupodcaststudio.application.document;

import java.awt.Color;
import java.nio.file.Path;

/** Request to render a visual crop from one PDF page using page-point coordinates. */
public record PdfCropRenderRequest(
        Path sourcePath,
        int pageNumber,
        double xMinPoints,
        double yMinPoints,
        double xMaxPoints,
        double yMaxPoints,
        double paddingPoints,
        int dpi,
        long maxPixelCount,
        Color backgroundColor,
        boolean renderAnnotations
) {
    public PdfCropRenderRequest {
        paddingPoints = Math.max(0.0, paddingPoints);
        dpi = dpi <= 0 ? PdfPageRenderRequest.DEFAULT_DPI : dpi;
        maxPixelCount = maxPixelCount <= 0 ? PdfPageRenderRequest.DEFAULT_MAX_PIXEL_COUNT : maxPixelCount;
        backgroundColor = backgroundColor == null ? Color.WHITE : backgroundColor;
    }
}

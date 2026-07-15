package com.marcosmoreiradev.docupodcaststudio.application.document;

import java.awt.Color;
import java.nio.file.Path;

/** Request to render one complete PDF page as a raster image. */
public record PdfPageRenderRequest(
        Path sourcePath,
        int pageNumber,
        int dpi,
        long maxPixelCount,
        Color backgroundColor,
        boolean renderAnnotations
) {
    public static final int DEFAULT_DPI = 144;
    public static final long DEFAULT_MAX_PIXEL_COUNT = 48_000_000L;

    public PdfPageRenderRequest {
        dpi = dpi <= 0 ? DEFAULT_DPI : dpi;
        maxPixelCount = maxPixelCount <= 0 ? DEFAULT_MAX_PIXEL_COUNT : maxPixelCount;
        backgroundColor = backgroundColor == null ? Color.WHITE : backgroundColor;
    }
}

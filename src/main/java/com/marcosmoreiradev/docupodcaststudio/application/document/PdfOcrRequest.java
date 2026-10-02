package com.marcosmoreiradev.docupodcaststudio.application.document;

import java.nio.file.Path;

/** Request to detect PDF text by OCR for one page. */
public record PdfOcrRequest(
        Path sourcePdf,
        int pageNumber,
        int dpi,
        long maxPixelCount,
        String languages,
        boolean forceOcr,
        Path cacheDirectory,
        int pageSegmentationMode,
        int normalizedRotationDegrees,
        PdfPageRegion cropRegion
) {
    public static final int DEFAULT_DPI = 300;
    public static final long DEFAULT_MAX_PIXEL_COUNT = 48_000_000L;
    public static final String DEFAULT_LANGUAGES = "spa+eng";

    public PdfOcrRequest {
        dpi = dpi <= 0 ? DEFAULT_DPI : dpi;
        maxPixelCount = maxPixelCount <= 0 ? DEFAULT_MAX_PIXEL_COUNT : maxPixelCount;
        languages = languages == null || languages.isBlank() ? DEFAULT_LANGUAGES : languages.strip();
        pageSegmentationMode = pageSegmentationMode <= 0 ? 6 : pageSegmentationMode;
        if (pageSegmentationMode > 13) throw new IllegalArgumentException("Unsupported Tesseract PSM");
        normalizedRotationDegrees = Math.floorMod(normalizedRotationDegrees, 360);
        if (normalizedRotationDegrees % 90 != 0) {
            throw new IllegalArgumentException("OCR rotation must be a multiple of 90");
        }
    }

    public PdfOcrRequest(Path sourcePdf, int pageNumber, int dpi,
                         long maxPixelCount, String languages,
                         boolean forceOcr, Path cacheDirectory,
                         int pageSegmentationMode,
                         int normalizedRotationDegrees) {
        this(sourcePdf, pageNumber, dpi, maxPixelCount, languages, forceOcr,
                cacheDirectory, pageSegmentationMode,
                normalizedRotationDegrees, null);
    }

    public PdfOcrRequest(Path sourcePdf, int pageNumber, int dpi, long maxPixelCount,
                         String languages, boolean forceOcr, Path cacheDirectory) {
        this(sourcePdf, pageNumber, dpi, maxPixelCount, languages, forceOcr,
                cacheDirectory, 6, 0, null);
    }
}

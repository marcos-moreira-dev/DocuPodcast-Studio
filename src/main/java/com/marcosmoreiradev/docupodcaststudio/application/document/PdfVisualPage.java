package com.marcosmoreiradev.docupodcaststudio.application.document;

/** Viewer descriptor for one PDF page in displayed, top-left coordinates. */
public record PdfVisualPage(
        int pageNumber,
        double widthPoints,
        double heightPoints,
        int rotationDegrees,
        int recommendedDpi
) {
    public PdfVisualPage {
        if (pageNumber <= 0) {
            throw new IllegalArgumentException("pageNumber must be positive");
        }
        widthPoints = Math.max(0.0, widthPoints);
        heightPoints = Math.max(0.0, heightPoints);
        rotationDegrees = PdfPageCoordinateTransform.normalizeRotation(rotationDegrees);
        recommendedDpi = recommendedDpi <= 0 ? PdfPageRenderRequest.DEFAULT_DPI : recommendedDpi;
    }

    public PdfVisualPage(int pageNumber, double widthPoints, double heightPoints, int recommendedDpi) {
        this(pageNumber, widthPoints, heightPoints, 0, recommendedDpi);
    }
}

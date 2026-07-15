package com.marcosmoreiradev.docupodcaststudio.application.document;

/** Future viewer descriptor for one PDF page. */
public record PdfVisualPage(int pageNumber, double widthPoints, double heightPoints, int recommendedDpi) {
    public PdfVisualPage {
        if (pageNumber <= 0) {
            throw new IllegalArgumentException("pageNumber must be positive");
        }
        widthPoints = Math.max(0.0, widthPoints);
        heightPoints = Math.max(0.0, heightPoints);
        recommendedDpi = recommendedDpi <= 0 ? PdfPageRenderRequest.DEFAULT_DPI : recommendedDpi;
    }
}

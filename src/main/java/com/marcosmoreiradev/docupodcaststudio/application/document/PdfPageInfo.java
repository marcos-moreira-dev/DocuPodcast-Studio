package com.marcosmoreiradev.docupodcaststudio.application.document;

/** Visual metadata for one source PDF page. */
public record PdfPageInfo(int pageNumber, double widthPoints, double heightPoints, int rotation) {
    public PdfPageInfo {
        if (pageNumber <= 0) {
            throw new IllegalArgumentException("pageNumber must be positive");
        }
        widthPoints = Math.max(0.0, widthPoints);
        heightPoints = Math.max(0.0, heightPoints);
    }
}

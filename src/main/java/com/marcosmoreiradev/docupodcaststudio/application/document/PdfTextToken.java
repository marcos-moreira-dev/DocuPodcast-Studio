package com.marcosmoreiradev.docupodcaststudio.application.document;

/** Word-like text item with PDF point coordinates for future read/highlight flows. */
public record PdfTextToken(
        String text,
        PdfPageRegion region,
        double confidence
) {
    public PdfTextToken {
        text = text == null ? "" : text.strip();
        confidence = Double.isFinite(confidence) ? Math.max(0.0, Math.min(1.0, confidence)) : 0.0;
    }
}

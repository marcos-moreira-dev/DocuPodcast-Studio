package com.marcosmoreiradev.docupodcaststudio.application.document;

/** OCR word with PDF point coordinates and confidence. */
public record PdfOcrWord(
        String text,
        PdfPageRegion region,
        double confidence
) {
    public PdfOcrWord {
        text = text == null ? "" : text.strip();
        confidence = Double.isFinite(confidence) ? Math.max(0.0, Math.min(1.0, confidence)) : 0.0;
    }
}

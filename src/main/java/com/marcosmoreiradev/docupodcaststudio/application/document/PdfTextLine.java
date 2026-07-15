package com.marcosmoreiradev.docupodcaststudio.application.document;

import java.util.List;

/** One visual/textual line in a future PDF text layer. */
public record PdfTextLine(
        int pageNumber,
        String text,
        PdfPageRegion region,
        List<PdfTextToken> tokens,
        double confidence,
        String dominantColor
) {
    public PdfTextLine(int pageNumber,
                       String text,
                       PdfPageRegion region,
                       List<PdfTextToken> tokens,
                       double confidence) {
        this(pageNumber, text, region, tokens, confidence, "");
    }

    public PdfTextLine {
        pageNumber = Math.max(0, pageNumber);
        text = text == null ? "" : text.strip();
        tokens = tokens == null ? List.of() : List.copyOf(tokens);
        confidence = Double.isFinite(confidence) ? Math.max(0.0, Math.min(1.0, confidence)) : 0.0;
        dominantColor = dominantColor == null ? "" : dominantColor.strip().toLowerCase(java.util.Locale.ROOT);
    }
}

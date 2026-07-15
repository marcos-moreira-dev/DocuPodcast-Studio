package com.marcosmoreiradev.docupodcaststudio.application.document;

import java.util.List;

/** OCR line grouped from words, ready to feed the internal reading text layer. */
public record PdfOcrLine(
        int pageNumber,
        String text,
        PdfPageRegion region,
        List<PdfOcrWord> words,
        double confidence,
        String dominantColor
) {
    public PdfOcrLine(int pageNumber,
                      String text,
                      PdfPageRegion region,
                      List<PdfOcrWord> words,
                      double confidence) {
        this(pageNumber, text, region, words, confidence, "");
    }

    public PdfOcrLine {
        pageNumber = Math.max(0, pageNumber);
        text = text == null ? "" : text.strip();
        words = words == null ? List.of() : List.copyOf(words);
        confidence = Double.isFinite(confidence) ? Math.max(0.0, Math.min(1.0, confidence)) : 0.0;
        dominantColor = dominantColor == null ? "" : dominantColor.strip().toLowerCase(java.util.Locale.ROOT);
    }

    public PdfTextLine toTextLine() {
        return new PdfTextLine(pageNumber, text, region,
                words.stream()
                        .map(word -> new PdfTextToken(word.text(), word.region(), word.confidence()))
                        .toList(),
                confidence,
                dominantColor);
    }
}

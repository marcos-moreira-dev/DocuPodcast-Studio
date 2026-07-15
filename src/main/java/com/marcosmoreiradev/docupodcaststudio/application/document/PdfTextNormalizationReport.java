package com.marcosmoreiradev.docupodcaststudio.application.document;

/** Result of conservative native-PDF text cleanup before document block building. */
public record PdfTextNormalizationReport(
        String normalizedText,
        int letterSpacingRepairs,
        int artificialLineBreakRepairs,
        int repeatedHeaderFooterLinesRemoved
) {
    public PdfTextNormalizationReport {
        normalizedText = normalizedText == null ? "" : normalizedText.strip();
        letterSpacingRepairs = Math.max(0, letterSpacingRepairs);
        artificialLineBreakRepairs = Math.max(0, artificialLineBreakRepairs);
        repeatedHeaderFooterLinesRemoved = Math.max(0, repeatedHeaderFooterLinesRemoved);
    }

    public boolean repaired() {
        return letterSpacingRepairs > 0
                || artificialLineBreakRepairs > 0
                || repeatedHeaderFooterLinesRemoved > 0;
    }
}

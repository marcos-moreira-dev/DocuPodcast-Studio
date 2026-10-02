package com.marcosmoreiradev.docupodcaststudio.domain.document.pdf;

/** Local measurements captured for the last successful preparation of one page. */
public record PdfPagePreparationMetrics(
        long elapsedMillis,
        long cpuMillis,
        long observedHeapBytes,
        boolean nativeExtractionUsed,
        boolean ocrUsed
) {
    public PdfPagePreparationMetrics {
        elapsedMillis = Math.max(0L, elapsedMillis);
        cpuMillis = Math.max(0L, cpuMillis);
        observedHeapBytes = Math.max(0L, observedHeapBytes);
    }

    public static PdfPagePreparationMetrics empty() {
        return new PdfPagePreparationMetrics(0L, 0L, 0L, false, false);
    }
}

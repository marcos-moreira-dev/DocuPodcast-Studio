package com.marcosmoreiradev.docupodcaststudio.domain.document.pdf;

import java.time.Instant;
import java.util.List;

/** Derived cross-page layout facts persisted in the canonical manifest. */
public record PdfDocumentAnalysisSummary(
        long revision,
        int observedPages,
        int maximumColumnCount,
        List<String> repeatedHeaders,
        List<String> repeatedFooters,
        Instant analyzedAt
) {
    public PdfDocumentAnalysisSummary {
        revision = Math.max(0L, revision);
        observedPages = Math.max(0, observedPages);
        maximumColumnCount = Math.max(0, maximumColumnCount);
        repeatedHeaders = repeatedHeaders == null ? List.of() : List.copyOf(repeatedHeaders);
        repeatedFooters = repeatedFooters == null ? List.of() : List.copyOf(repeatedFooters);
        analyzedAt = analyzedAt == null ? Instant.EPOCH : analyzedAt;
    }

    public static PdfDocumentAnalysisSummary empty() {
        return new PdfDocumentAnalysisSummary(0, 0, 0, List.of(), List.of(), Instant.EPOCH);
    }
}

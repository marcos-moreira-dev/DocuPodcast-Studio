package com.marcosmoreiradev.docupodcaststudio.domain.document.pdf;

/** Safe behavior when no mathematical recognition/speech path is available. */
public enum PdfMathFallbackPolicy {
    ANNOUNCE_ONLY,
    SKIP,
    REQUIRE_REVIEW
}

package com.marcosmoreiradev.docupodcaststudio.domain.document.pdf;

/** Product decision describing how one PDF object may enter narration. */
public enum PdfObjectNarrationPolicy {
    READ_EXACT,
    READ_ALL,
    READ_TEXTUAL_CONTENT,
    DESCRIBE_BRIEFLY,
    SUMMARIZE,
    ANNOUNCE_ONLY,
    SKIP,
    REQUIRE_REVIEW
}

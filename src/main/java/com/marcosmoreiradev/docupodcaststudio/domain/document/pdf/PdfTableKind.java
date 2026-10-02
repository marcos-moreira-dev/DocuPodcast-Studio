package com.marcosmoreiradev.docupodcaststudio.domain.document.pdf;

/** Semantic class used to choose a safe table narration strategy. */
public enum PdfTableKind {
    PROSE,
    KEY_VALUE,
    MIXED,
    NUMERIC,
    MATRIX,
    MATH,
    UNKNOWN
}

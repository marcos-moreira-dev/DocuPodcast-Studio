package com.marcosmoreiradev.docupodcaststudio.domain.document.pdf;

/** Semantic role of a PDF page, independent from individual region types. */
public enum PdfPageRole {
    CONTENT,
    INDEX,
    BIBLIOGRAPHY,
    CATALOG,
    VISUAL_REFERENCE,
    UNKNOWN
}

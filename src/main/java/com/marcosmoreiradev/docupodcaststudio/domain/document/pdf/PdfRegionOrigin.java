package com.marcosmoreiradev.docupodcaststudio.domain.document.pdf;

/** Evidence source used to prepare a PDF region. */
public enum PdfRegionOrigin {
    OCR_LOCAL,
    NATIVE_TEXT,
    HYBRID,
    VLM_SEMANTIC
}

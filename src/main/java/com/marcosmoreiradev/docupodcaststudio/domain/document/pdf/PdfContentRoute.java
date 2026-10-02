package com.marcosmoreiradev.docupodcaststudio.domain.document.pdf;

/** Content-resolution route selected after page layout has frozen identity/order. */
public enum PdfContentRoute {
    OCR_SAFE,
    VLM_MIXED,
    VLM_VISUAL,
    VLM_STRUCTURED
}

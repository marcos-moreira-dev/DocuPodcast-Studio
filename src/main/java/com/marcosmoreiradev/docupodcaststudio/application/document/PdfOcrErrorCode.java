package com.marcosmoreiradev.docupodcaststudio.application.document;

/** Stable OCR error codes for local PDF text detection. */
public enum PdfOcrErrorCode {
    INVALID_REQUEST,
    TESSERACT_NOT_FOUND,
    PDF_RENDER_FAILED,
    OCR_FAILED,
    CACHE_FAILED
}

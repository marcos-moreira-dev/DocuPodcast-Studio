package com.marcosmoreiradev.docupodcaststudio.application.document;

/** OCR policy for resolving the internal PDF text layer. */
public enum PdfTextResolutionPolicy {
    NATIVE_ONLY,
    OCR_WHEN_UNAVAILABLE,
    FORCE_OCR
}

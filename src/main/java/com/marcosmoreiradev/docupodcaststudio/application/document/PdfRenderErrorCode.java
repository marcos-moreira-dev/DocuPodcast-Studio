package com.marcosmoreiradev.docupodcaststudio.application.document;

/** Stable error codes for the embedded PDF rendering engine. */
public enum PdfRenderErrorCode {
    INVALID_PDF,
    PASSWORD_REQUIRED,
    PAGE_OUT_OF_RANGE,
    TOO_LARGE,
    RENDER_FAILED,
    UNSUPPORTED_FEATURE
}

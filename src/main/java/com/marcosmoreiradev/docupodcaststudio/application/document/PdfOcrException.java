package com.marcosmoreiradev.docupodcaststudio.application.document;

import java.io.IOException;
import java.util.Objects;

/** Controlled exception raised by local PDF OCR engines. */
public final class PdfOcrException extends IOException {
    private final PdfOcrErrorCode code;

    public PdfOcrException(PdfOcrErrorCode code, String message) {
        super(message);
        this.code = Objects.requireNonNullElse(code, PdfOcrErrorCode.OCR_FAILED);
    }

    public PdfOcrException(PdfOcrErrorCode code, String message, Throwable cause) {
        super(message, cause);
        this.code = Objects.requireNonNullElse(code, PdfOcrErrorCode.OCR_FAILED);
    }

    public PdfOcrErrorCode code() {
        return code;
    }
}

package com.marcosmoreiradev.docupodcaststudio.application.document;

import java.io.IOException;
import java.util.Objects;

/** Controlled exception raised by PDF render engines. */
public final class PdfRenderException extends IOException {
    private final PdfRenderErrorCode code;

    public PdfRenderException(PdfRenderErrorCode code, String message) {
        super(message);
        this.code = Objects.requireNonNullElse(code, PdfRenderErrorCode.RENDER_FAILED);
    }

    public PdfRenderException(PdfRenderErrorCode code, String message, Throwable cause) {
        super(message, cause);
        this.code = Objects.requireNonNullElse(code, PdfRenderErrorCode.RENDER_FAILED);
    }

    public PdfRenderErrorCode code() {
        return code;
    }
}

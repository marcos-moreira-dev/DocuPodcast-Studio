package com.marcosmoreiradev.docupodcaststudio.application.document;

import java.io.IOException;
import java.util.Map;

/** Atomic PAGE_SEMANTIC_READING protocol failure. */
public final class PdfSemanticProtocolException extends IOException {
    public enum Kind {
        INCOMPLETE,
        INVALID
    }

    private final Kind kind;
    private final Map<String, String> diagnostics;

    public PdfSemanticProtocolException(Kind kind, String message) {
        this(kind, message, Map.of(), null);
    }

    public PdfSemanticProtocolException(Kind kind, String message,
                                        Throwable cause) {
        this(kind, message, Map.of(), cause);
    }

    public PdfSemanticProtocolException(Kind kind, String message,
                                        Map<String, String> diagnostics,
                                        Throwable cause) {
        super(message, cause);
        this.kind = kind == null ? Kind.INVALID : kind;
        this.diagnostics = diagnostics == null ? Map.of() : Map.copyOf(diagnostics);
    }

    public Kind kind() {
        return kind;
    }

    public Map<String, String> diagnostics() {
        return diagnostics;
    }
}

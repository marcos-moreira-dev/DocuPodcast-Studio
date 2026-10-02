package com.marcosmoreiradev.docupodcaststudio.application.document;

import java.io.IOException;
import java.util.Map;

/** Atomic rejection when semantic coverage remains insufficient. */
public final class PdfSemanticCoverageException extends IOException {
    private final PdfSemanticCoverageResult result;
    private final String rawOutput;
    private final Map<String, String> diagnostics;

    public PdfSemanticCoverageException(String message,
                                        PdfSemanticCoverageResult result) {
        this(message, result, "");
    }

    public PdfSemanticCoverageException(String message,
                                        PdfSemanticCoverageResult result,
                                        String rawOutput) {
        this(message, result, rawOutput, Map.of());
    }

    public PdfSemanticCoverageException(String message,
                                        PdfSemanticCoverageResult result,
                                        String rawOutput,
                                        Map<String, String> diagnostics) {
        super(message);
        this.result = result;
        this.rawOutput = rawOutput == null ? "" : rawOutput;
        this.diagnostics = diagnostics == null ? Map.of() : Map.copyOf(diagnostics);
    }

    public PdfSemanticCoverageResult result() {
        return result;
    }

    public String rawOutput() {
        return rawOutput;
    }

    public Map<String, String> diagnostics() {
        return diagnostics;
    }
}

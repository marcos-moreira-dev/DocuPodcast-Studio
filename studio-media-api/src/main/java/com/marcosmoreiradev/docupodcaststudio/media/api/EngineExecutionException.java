package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

/** IOException carrying a stable machine-readable engine failure code. */
public final class EngineExecutionException extends IOException {
    private final EngineDiagnosticCode code;
    private final Map<String, String> diagnostics;

    public EngineExecutionException(EngineDiagnosticCode code, String message) {
        this(code, message, Map.of(), null);
    }

    public EngineExecutionException(EngineDiagnosticCode code, String message,
                                    Map<String, String> diagnostics) {
        this(code, message, diagnostics, null);
    }

    public EngineExecutionException(EngineDiagnosticCode code, String message,
                                    Map<String, String> diagnostics, Throwable cause) {
        super(message, cause);
        this.code = Objects.requireNonNull(code, "code");
        this.diagnostics = diagnostics == null ? Map.of() : Map.copyOf(diagnostics);
    }

    public EngineDiagnosticCode code() {
        return code;
    }

    public Map<String, String> diagnostics() {
        return diagnostics;
    }
}

package com.marcosmoreiradev.docupodcaststudio.application.reading;

import com.marcosmoreiradev.docupodcaststudio.media.api.EngineExecutionException;

import java.io.IOException;
import java.util.Map;

/** Identifies the exact narration segment whose derived translation failed. */
public final class NarrationTranslationException extends IOException {
    private final String segmentId;
    private final String fingerprint;
    private final String reason;
    private final Map<String, String> diagnostics;

    public NarrationTranslationException(String segmentId, String fingerprint,
                                         Throwable cause) {
        super(message(segmentId, cause), cause);
        this.segmentId = segmentId == null ? "" : segmentId;
        this.fingerprint = fingerprint == null ? "" : fingerprint;
        EngineExecutionException engine = engineFailure(cause);
        this.reason = engine == null ? cause == null
                ? "UNKNOWN" : cause.getClass().getSimpleName()
                : engine.code().name();
        this.diagnostics = engine == null ? Map.of() : engine.diagnostics();
    }

    public String segmentId() { return segmentId; }
    public String fingerprint() { return fingerprint; }
    public String stage() { return "TRANSLATION"; }
    public String reason() { return reason; }
    public Map<String, String> diagnostics() { return diagnostics; }

    private static String message(String segmentId, Throwable cause) {
        String detail = cause == null || cause.getMessage() == null
                ? "fallo desconocido" : cause.getMessage();
        return "No se pudo traducir el fragmento "
                + (segmentId == null ? "" : segmentId) + ": " + detail;
    }

    private static EngineExecutionException engineFailure(Throwable failure) {
        Throwable cursor = failure;
        while (cursor != null) {
            if (cursor instanceof EngineExecutionException engine) return engine;
            cursor = cursor.getCause();
        }
        return null;
    }
}

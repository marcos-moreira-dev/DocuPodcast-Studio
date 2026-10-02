package com.marcosmoreiradev.docupodcaststudio.application.theatrepackage;

import java.util.Objects;

public record TheatreRefreshDiagnostic(
        String code,
        TheatreRefreshDiagnosticSeverity severity,
        String logicalId,
        String message
) {
    public TheatreRefreshDiagnostic {
        code = required(code, "code");
        severity = Objects.requireNonNull(severity, "severity");
        logicalId = Objects.requireNonNullElse(logicalId, "").strip();
        message = required(message, "message");
    }

    private static String required(String value, String field) {
        String normalized = Objects.requireNonNullElse(value, "").strip();
        if (normalized.isBlank()) throw new IllegalArgumentException(field + " is required");
        return normalized;
    }
}

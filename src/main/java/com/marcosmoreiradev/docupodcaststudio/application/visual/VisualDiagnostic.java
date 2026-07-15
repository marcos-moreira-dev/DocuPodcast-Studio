package com.marcosmoreiradev.docupodcaststudio.application.visual;

/** Human-readable visual production diagnostic. */
public record VisualDiagnostic(
        String code,
        String message,
        String fragmentId,
        boolean blocking
) {
    public VisualDiagnostic {
        code = normalize(code).isBlank() ? "VISUAL_DIAGNOSTIC" : normalize(code);
        message = normalize(message);
        fragmentId = normalize(fragmentId);
    }

    public static VisualDiagnostic warning(String code, String message, String fragmentId) {
        return new VisualDiagnostic(code, message, fragmentId, false);
    }

    public static VisualDiagnostic blocker(String code, String message, String fragmentId) {
        return new VisualDiagnostic(code, message, fragmentId, true);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}

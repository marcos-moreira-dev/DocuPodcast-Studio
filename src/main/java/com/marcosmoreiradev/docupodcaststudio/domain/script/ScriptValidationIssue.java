package com.marcosmoreiradev.docupodcaststudio.domain.script;

import java.util.Objects;

/** Validation finding associated with the whole script or with a segment. */
public record ScriptValidationIssue(
        ScriptValidationIssueLevel level,
        String code,
        String message,
        String segmentId
) {
    public ScriptValidationIssue {
        level = Objects.requireNonNullElse(level, ScriptValidationIssueLevel.INFO);
        code = normalizeRequired(code, "code");
        message = normalizeRequired(message, "message");
        segmentId = segmentId == null ? "" : segmentId.strip();
    }

    public static ScriptValidationIssue warning(String code, String message, String segmentId) {
        return new ScriptValidationIssue(ScriptValidationIssueLevel.WARNING, code, message, segmentId);
    }

    public static ScriptValidationIssue error(String code, String message, String segmentId) {
        return new ScriptValidationIssue(ScriptValidationIssueLevel.ERROR, code, message, segmentId);
    }

    private static String normalizeRequired(String value, String field) {
        String normalized = value == null ? "" : value.strip();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return normalized;
    }
}

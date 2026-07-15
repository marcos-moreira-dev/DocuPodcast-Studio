package com.marcosmoreiradev.docupodcaststudio.application.project;

import java.util.Objects;

/** One actionable integrity finding for a DocuPodcast project. */
public record ProjectIntegrityIssue(
        String code,
        ProjectIntegritySeverity severity,
        String target,
        String message,
        String repairSuggestion
) {
    public ProjectIntegrityIssue {
        code = tokenOrDefault(code, "PROJECT_INTEGRITY");
        severity = Objects.requireNonNullElse(severity, ProjectIntegritySeverity.WARNING);
        target = normalize(target);
        message = required(message, "message");
        repairSuggestion = normalize(repairSuggestion);
    }

    public static ProjectIntegrityIssue info(String code, String target, String message, String repairSuggestion) {
        return new ProjectIntegrityIssue(code, ProjectIntegritySeverity.INFO, target, message, repairSuggestion);
    }

    public static ProjectIntegrityIssue warning(String code, String target, String message, String repairSuggestion) {
        return new ProjectIntegrityIssue(code, ProjectIntegritySeverity.WARNING, target, message, repairSuggestion);
    }

    public static ProjectIntegrityIssue error(String code, String target, String message, String repairSuggestion) {
        return new ProjectIntegrityIssue(code, ProjectIntegritySeverity.ERROR, target, message, repairSuggestion);
    }

    public boolean blocking() {
        return severity.blocking();
    }

    public String displayLine() {
        String prefix = target.isBlank() ? code : code + " · " + target;
        if (repairSuggestion.isBlank()) {
            return severity.displayName() + " · " + prefix + ": " + message;
        }
        return severity.displayName() + " · " + prefix + ": " + message + " Sugerencia: " + repairSuggestion;
    }

    private static String tokenOrDefault(String value, String fallback) {
        String normalized = normalize(value);
        if (normalized.isBlank()) {
            return fallback;
        }
        if (normalized.chars().anyMatch(Character::isWhitespace)) {
            return normalized.replaceAll("\\s+", "_").toUpperCase(java.util.Locale.ROOT);
        }
        return normalized;
    }

    private static String required(String value, String field) {
        String normalized = normalize(value);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return normalized;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}

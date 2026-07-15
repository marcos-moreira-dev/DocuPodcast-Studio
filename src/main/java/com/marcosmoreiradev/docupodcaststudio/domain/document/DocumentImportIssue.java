package com.marcosmoreiradev.docupodcaststudio.domain.document;

import java.util.Objects;

/** Human-readable issue attached to an imported document. */
public record DocumentImportIssue(
        DocumentImportIssueLevel level,
        String code,
        String message,
        String blockId
) {
    public DocumentImportIssue {
        level = Objects.requireNonNullElse(level, DocumentImportIssueLevel.INFO);
        code = normalizeToken(code, "code");
        message = normalizeText(message, "message");
        blockId = blockId == null ? "" : blockId.strip();
    }

    public static DocumentImportIssue info(String code, String message) {
        return new DocumentImportIssue(DocumentImportIssueLevel.INFO, code, message, "");
    }

    public static DocumentImportIssue warning(String code, String message) {
        return new DocumentImportIssue(DocumentImportIssueLevel.WARNING, code, message, "");
    }

    public static DocumentImportIssue warning(String code, String message, String blockId) {
        return new DocumentImportIssue(DocumentImportIssueLevel.WARNING, code, message, blockId);
    }

    public static DocumentImportIssue error(String code, String message) {
        return new DocumentImportIssue(DocumentImportIssueLevel.ERROR, code, message, "");
    }

    /** Compatibility alias used by presentation code that speaks in severity terms. */
    public DocumentImportIssueLevel severity() {
        return level;
    }

    private static String normalizeToken(String value, String field) {
        String normalized = value == null ? "" : value.strip();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        if (normalized.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException(field + " must not contain whitespace");
        }
        return normalized;
    }

    private static String normalizeText(String value, String field) {
        String normalized = value == null ? "" : value.strip();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return normalized;
    }
}

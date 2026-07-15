package com.marcosmoreiradev.docupodcaststudio.domain.script;

/** Immutable selection range inside one narration segment. */
public record ScriptTextRange(String segmentId, int startOffset, int endOffset) {
    public ScriptTextRange {
        segmentId = token(segmentId, "segmentId");
        startOffset = Math.max(0, startOffset);
        if (endOffset < startOffset) {
            throw new IllegalArgumentException("endOffset must be >= startOffset");
        }
    }

    public boolean collapsed() {
        return startOffset == endOffset;
    }

    private static String token(String value, String field) {
        String normalized = value == null ? "" : value.strip();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        if (normalized.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException(field + " must not contain whitespace");
        }
        return normalized;
    }
}

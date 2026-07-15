package com.marcosmoreiradev.docupodcaststudio.domain.document;

/** Sentence-level span detected from an imported document block for precise user selection. */
public record DocumentSentenceSpan(
        String id,
        int index,
        DocumentTextRange range,
        String text
) {
    public DocumentSentenceSpan {
        id = token(id, "id");
        if (index < 0) {
            throw new IllegalArgumentException("index must be >= 0");
        }
        if (range == null) {
            throw new IllegalArgumentException("range is required");
        }
        text = text == null ? "" : text.strip();
    }

    public String blockId() {
        return range.blockId();
    }

    public boolean blank() {
        return text.isBlank();
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

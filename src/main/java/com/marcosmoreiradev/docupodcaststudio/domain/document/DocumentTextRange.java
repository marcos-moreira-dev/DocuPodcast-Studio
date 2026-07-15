package com.marcosmoreiradev.docupodcaststudio.domain.document;

/**
 * Text range selected inside one imported document block. This is a project-side
 * production anchor; it never modifies the source Word/DOCX content.
 */
public record DocumentTextRange(String blockId, int startOffset, int endOffset) {
    public DocumentTextRange {
        blockId = token(blockId, "blockId");
        startOffset = Math.max(0, startOffset);
        if (endOffset < startOffset) {
            throw new IllegalArgumentException("endOffset must be >= startOffset");
        }
    }

    public int length() {
        return endOffset - startOffset;
    }

    public boolean collapsed() {
        return startOffset == endOffset;
    }

    public boolean overlaps(DocumentTextRange other) {
        if (other == null || !blockId.equals(other.blockId())) {
            return false;
        }
        return startOffset < other.endOffset() && other.startOffset() < endOffset;
    }

    public String displayLabel() {
        return blockId + " [" + startOffset + ".." + endOffset + "]";
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

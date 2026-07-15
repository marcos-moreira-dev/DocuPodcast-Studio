package com.marcosmoreiradev.docupodcaststudio.domain.study;

/** Optional duration override for one source DOCX table. */
public record DocumentTableSlideConfiguration(String blockId, double durationSeconds) {
    public DocumentTableSlideConfiguration {
        blockId = blockId == null ? "" : blockId.strip();
        if (blockId.isBlank() || blockId.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException("blockId is required and must not contain whitespace");
        }
        durationSeconds = clamp(durationSeconds);
    }

    public static double clamp(double value) {
        return Double.isFinite(value) ? Math.max(2.0, Math.min(60.0, value)) : 6.0;
    }
}

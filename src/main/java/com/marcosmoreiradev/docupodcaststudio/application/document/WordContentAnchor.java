package com.marcosmoreiradev.docupodcaststudio.application.document;

import java.util.Map;

/** Source anchor for a block-backed document. */
public record WordContentAnchor(String blockId, Map<String, String> metadata)
        implements DocumentContentAnchor {
    public WordContentAnchor {
        blockId = token(blockId);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public WordContentAnchor(String blockId) {
        this(blockId, Map.of());
    }

    private static String token(String value) {
        String normalized = value == null ? "" : value.strip();
        if (normalized.isBlank() || normalized.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException("blockId must be a non-blank token");
        }
        return normalized;
    }
}

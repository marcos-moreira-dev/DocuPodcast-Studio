package com.marcosmoreiradev.docupodcaststudio.application.documentstudy;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlockType;

import java.util.Map;
import java.util.Objects;

/** Secondary document support item shown in study mode without becoming a primary fragment. */
public record DocumentStudySupportItem(
        String blockId,
        DocumentBlockType blockType,
        String label,
        String text,
        String sourceLocation,
        Map<String, String> metadata
) {
    public DocumentStudySupportItem {
        blockId = normalizeToken(blockId);
        blockType = Objects.requireNonNullElse(blockType, DocumentBlockType.IMAGE_NOTICE);
        label = normalize(label);
        text = normalize(text);
        sourceLocation = normalize(sourceLocation);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    private static String normalizeToken(String value) {
        String normalized = normalize(value);
        if (!normalized.isBlank() && normalized.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException("token must not contain whitespace: " + normalized);
        }
        return normalized;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}

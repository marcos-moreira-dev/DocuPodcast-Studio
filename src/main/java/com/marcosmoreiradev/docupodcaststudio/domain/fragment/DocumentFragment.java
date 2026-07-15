package com.marcosmoreiradev.docupodcaststudio.domain.fragment;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentTextRange;

import java.util.Map;
import java.util.Objects;

/** Cross-media fragment derived from source document blocks and compatible narration segments. */
public record DocumentFragment(
        FragmentId fragmentId,
        int order,
        String text,
        String sourceLocation,
        DocumentTextRange sourceRange,
        String sourceBlockId,
        String segmentId,
        String contentHash,
        FragmentStatus status,
        Map<String, String> metadata
) {
    public DocumentFragment {
        fragmentId = Objects.requireNonNull(fragmentId, "fragmentId");
        if (order < 0) {
            throw new IllegalArgumentException("order must be >= 0");
        }
        text = normalize(text);
        sourceLocation = normalize(sourceLocation);
        sourceBlockId = normalizeToken(sourceBlockId);
        segmentId = normalizeToken(segmentId);
        contentHash = normalize(contentHash);
        status = status == null ? FragmentStatus.SOURCE_ONLY : status;
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public boolean hasSourceBlock() {
        return !sourceBlockId.isBlank();
    }

    public boolean hasSegment() {
        return !segmentId.isBlank();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }

    private static String normalizeToken(String value) {
        String normalized = normalize(value);
        if (!normalized.isBlank() && normalized.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException("token must not contain whitespace: " + normalized);
        }
        return normalized;
    }
}

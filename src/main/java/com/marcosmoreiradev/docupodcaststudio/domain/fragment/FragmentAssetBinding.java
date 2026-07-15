package com.marcosmoreiradev.docupodcaststudio.domain.fragment;

import java.util.Map;
import java.util.Objects;

/** Derived relation between a fragment and an asset, job state, cue or semantic layer. */
public record FragmentAssetBinding(
        String id,
        FragmentId fragmentId,
        String assetId,
        FragmentAssetRole role,
        FragmentAssetSource source,
        String assetPath,
        String status,
        String targetId,
        Map<String, String> metadata
) {
    public FragmentAssetBinding {
        id = token(id, "id");
        fragmentId = Objects.requireNonNull(fragmentId, "fragmentId");
        assetId = normalizeToken(assetId);
        role = Objects.requireNonNullElse(role, FragmentAssetRole.NOTE);
        source = Objects.requireNonNullElse(source, FragmentAssetSource.UNKNOWN);
        assetPath = normalizePath(assetPath);
        status = normalize(status);
        targetId = normalizeToken(targetId);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        if (assetId.isBlank() && assetPath.isBlank() && targetId.isBlank()) {
            throw new IllegalArgumentException("assetId, assetPath or targetId is required");
        }
    }

    private static String token(String value, String field) {
        String normalized = normalizeToken(value);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return normalized;
    }

    private static String normalizeToken(String value) {
        String normalized = normalize(value);
        if (!normalized.isBlank() && normalized.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException("token must not contain whitespace: " + normalized);
        }
        return normalized;
    }

    private static String normalizePath(String value) {
        return normalize(value).replace('\\', '/');
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}

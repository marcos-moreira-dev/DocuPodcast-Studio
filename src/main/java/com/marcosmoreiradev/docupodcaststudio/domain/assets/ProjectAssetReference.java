package com.marcosmoreiradev.docupodcaststudio.domain.assets;

import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Portable reference to a project asset.
 *
 * <p>The path must be relative to the project folder. Absolute paths, URLs and parent traversal are rejected
 * so exported projects remain portable and safe.</p>
 */
public record ProjectAssetReference(
        String id,
        ProjectAssetKind kind,
        String displayName,
        String relativePath,
        String mimeType,
        String purpose,
        String checksum,
        String notes
) {
    private static final Pattern WINDOWS_ABSOLUTE = Pattern.compile("^[A-Za-z]:[\\\\/].*");

    public ProjectAssetReference {
        id = requireToken(id, "id");
        kind = Objects.requireNonNull(kind, "kind");
        displayName = requireText(displayName, "displayName");
        relativePath = normalizeAndValidateRelativePath(relativePath);
        mimeType = normalizeOptional(mimeType);
        purpose = normalizeOptional(purpose);
        checksum = normalizeOptional(checksum);
        notes = normalizeOptional(notes);
    }

    public static ProjectAssetReference sourceDocument(String id, String displayName, String relativePath, String mimeType) {
        return new ProjectAssetReference(id, ProjectAssetKind.SOURCE_DOCUMENT, displayName, relativePath, mimeType,
                "Documento fuente original", "", "");
    }

    public boolean isAudio() {
        return kind == ProjectAssetKind.AUDIO_CLIP || kind == ProjectAssetKind.AUDIO_FINAL;
    }

    public boolean isImage() {
        return kind == ProjectAssetKind.IMAGE
                || kind == ProjectAssetKind.THUMBNAIL
                || kind == ProjectAssetKind.STUDY_SOURCE_CROP
                || kind == ProjectAssetKind.STUDY_PROBLEM_IMAGE
                || kind == ProjectAssetKind.STUDY_SOLUTION_IMAGE;
    }

    public boolean isVideo() {
        return kind == ProjectAssetKind.VIDEO_SOURCE;
    }

    private static String normalizeAndValidateRelativePath(String value) {
        String raw = requireText(value, "relativePath");
        if (raw.startsWith("~")) {
            throw new IllegalArgumentException("relativePath must not start with '~'");
        }
        if (raw.startsWith("/") || raw.startsWith("\\\\")) {
            throw new IllegalArgumentException("relativePath must not be absolute");
        }
        String lower = raw.toLowerCase();
        if (lower.startsWith("file:") || lower.startsWith("http:") || lower.startsWith("https:") || lower.contains("://")) {
            throw new IllegalArgumentException("relativePath must not be a URL");
        }
        if (WINDOWS_ABSOLUTE.matcher(raw).matches()) {
            throw new IllegalArgumentException("relativePath must not be a Windows absolute path");
        }
        String normalized = raw.replace('\\', '/');
        if (normalized.startsWith("/") || normalized.endsWith("/")) {
            throw new IllegalArgumentException("relativePath must not start or end with '/'");
        }
        if (normalized.contains("//")) {
            throw new IllegalArgumentException("relativePath must not contain empty path segments");
        }
        boolean hasTraversal = Arrays.stream(normalized.split("/"))
                .anyMatch(segment -> segment.equals("..") || segment.equals("."));
        if (hasTraversal) {
            throw new IllegalArgumentException("relativePath must not contain '.' or '..' segments");
        }
        return normalized;
    }

    private static String requireToken(String value, String field) {
        String normalized = requireText(value, field);
        if (normalized.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException(field + " must not contain whitespace");
        }
        return normalized;
    }

    private static String requireText(String value, String field) {
        String normalized = normalizeOptional(value);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return normalized;
    }

    private static String normalizeOptional(String value) {
        return value == null ? "" : value.trim();
    }
}

package com.marcosmoreiradev.docupodcaststudio.domain.theatrepackage;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/** One normalized, content-addressed input from a theatre package. */
public record TheatrePackageEntry(
        String logicalId,
        TheatrePackageAssetKind kind,
        String relativePath,
        String sha256,
        long size,
        Map<String, String> metadata
) {
    private static final Pattern WINDOWS_ABSOLUTE = Pattern.compile("^[A-Za-z]:[\\\\/].*");

    public TheatrePackageEntry {
        logicalId = required(logicalId, "logicalId");
        if (logicalId.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException("logicalId must not contain whitespace");
        }
        kind = Objects.requireNonNull(kind, "kind");
        relativePath = safeRelativePath(relativePath);
        sha256 = required(sha256, "sha256").toLowerCase(java.util.Locale.ROOT);
        if (!sha256.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("sha256 must contain 64 hexadecimal characters");
        }
        if (size < 0) throw new IllegalArgumentException("size must be non-negative");
        LinkedHashMap<String, String> copy = new LinkedHashMap<>();
        if (metadata != null) metadata.forEach((key, value) -> {
            String normalizedKey = required(key, "metadata key");
            copy.put(normalizedKey, value == null ? "" : value.strip());
        });
        metadata = Map.copyOf(copy);
    }

    public String metadata(String key) {
        return metadata.getOrDefault(key, "");
    }

    private static String safeRelativePath(String value) {
        String raw = required(value, "relativePath");
        if (raw.startsWith("~") || raw.startsWith("/") || raw.startsWith("\\\\")
                || WINDOWS_ABSOLUTE.matcher(raw).matches() || raw.contains("://")) {
            throw new IllegalArgumentException("relativePath must stay inside the theatre package");
        }
        String normalized = raw.replace('\\', '/');
        if (normalized.startsWith("/") || normalized.endsWith("/") || normalized.contains("//")
                || Arrays.stream(normalized.split("/")).anyMatch(part -> part.equals(".") || part.equals(".."))) {
            throw new IllegalArgumentException("relativePath contains unsafe path segments");
        }
        return normalized;
    }

    private static String required(String value, String field) {
        String normalized = Objects.requireNonNullElse(value, "").strip();
        if (normalized.isBlank()) throw new IllegalArgumentException(field + " is required");
        return normalized;
    }
}

package com.marcosmoreiradev.docupodcaststudio.application.export;

import java.nio.file.Path;
import java.util.Objects;

/** Metadata for a file included in an auditable project bundle. */
public record BundleArtifactMetadata(
        Path relativePath,
        long sizeBytes,
        String sha256
) {
    public BundleArtifactMetadata {
        relativePath = Objects.requireNonNull(relativePath, "relativePath");
        sha256 = Objects.requireNonNull(sha256, "sha256").trim();
        if (sha256.isBlank()) {
            throw new IllegalArgumentException("sha256 is required");
        }
        if (sizeBytes < 0) {
            throw new IllegalArgumentException("sizeBytes must be non-negative");
        }
    }
}

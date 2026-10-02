package com.marcosmoreiradev.docupodcaststudio.application.document;

import java.nio.file.Path;

/** Stable reference to one canonical PDF V2 workspace. */
public record PreparedPdfWorkspaceRef(
        Path projectRoot,
        Path sourcePath,
        String sourceSha256
) {
    public PreparedPdfWorkspaceRef {
        if (projectRoot == null) throw new IllegalArgumentException("projectRoot is required");
        if (sourcePath == null) throw new IllegalArgumentException("sourcePath is required");
        projectRoot = projectRoot.toAbsolutePath().normalize();
        sourcePath = sourcePath.toAbsolutePath().normalize();
        sourceSha256 = sourceSha256 == null ? "" : sourceSha256.strip().toLowerCase(java.util.Locale.ROOT);
        if (!sourceSha256.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("sourceSha256 must contain 64 hexadecimal characters");
        }
    }
}

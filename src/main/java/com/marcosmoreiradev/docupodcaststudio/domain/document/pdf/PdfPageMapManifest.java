package com.marcosmoreiradev.docupodcaststudio.domain.document.pdf;

import java.time.Instant;
import java.util.List;

/** Manifest for independently removable PdfPageMap V1 sidecars. */
public record PdfPageMapManifest(
        int schemaVersion,
        String sourceSha256,
        String builderSignature,
        List<Integer> pageNumbers,
        Instant updatedAt
) {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    public PdfPageMapManifest {
        if (schemaVersion != CURRENT_SCHEMA_VERSION) throw new IllegalArgumentException("Unsupported PageMap manifest");
        sourceSha256 = sourceSha256 == null ? "" : sourceSha256.strip();
        builderSignature = builderSignature == null ? "" : builderSignature.strip();
        if (sourceSha256.isBlank() || builderSignature.isBlank()) throw new IllegalArgumentException("PageMap identity is required");
        pageNumbers = pageNumbers == null ? List.of() : pageNumbers.stream().filter(value -> value != null && value > 0).distinct().sorted().toList();
        updatedAt = updatedAt == null ? Instant.EPOCH : updatedAt;
    }
}

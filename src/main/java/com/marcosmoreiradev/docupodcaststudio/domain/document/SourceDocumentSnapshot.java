package com.marcosmoreiradev.docupodcaststudio.domain.document;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Immutable fingerprint of the read-only source document as imported into the project.
 * It lets the app compare a refreshed external file without editing that file.
 */
public record SourceDocumentSnapshot(
        SourceDocumentFormat format,
        Path sourcePath,
        int blockCount,
        long narratableBlockCount,
        long wordCount,
        String contentHash
) {
    public SourceDocumentSnapshot {
        format = Objects.requireNonNullElse(format, SourceDocumentFormat.UNKNOWN);
        sourcePath = Objects.requireNonNull(sourcePath, "sourcePath").toAbsolutePath().normalize();
        if (blockCount < 0) {
            throw new IllegalArgumentException("blockCount must be >= 0");
        }
        if (narratableBlockCount < 0) {
            throw new IllegalArgumentException("narratableBlockCount must be >= 0");
        }
        if (wordCount < 0) {
            throw new IllegalArgumentException("wordCount must be >= 0");
        }
        contentHash = requireHash(contentHash);
    }

    public static SourceDocumentSnapshot from(ReadableDocument document) {
        Objects.requireNonNull(document, "document");
        return new SourceDocumentSnapshot(
                document.format(),
                document.sourcePath(),
                document.blocks().size(),
                document.narratableBlockCount(),
                document.wordCount(),
                fingerprint(document));
    }

    public boolean sameContentAs(SourceDocumentSnapshot other) {
        return other != null && contentHash.equals(other.contentHash);
    }

    private static String fingerprint(ReadableDocument document) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            update(digest, document.title());
            update(digest, document.format().name());
            for (DocumentBlock block : document.blocks()) {
                update(digest, block.id());
                update(digest, block.type().name());
                update(digest, Boolean.toString(block.narratable()));
                update(digest, block.text());
                update(digest, block.originalStyle());
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no disponible", ex);
        }
    }

    private static void update(MessageDigest digest, String value) {
        digest.update((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
        digest.update((byte) 0);
    }

    private static String requireHash(String value) {
        String normalized = value == null ? "" : value.strip();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("contentHash is required");
        }
        return normalized;
    }
}

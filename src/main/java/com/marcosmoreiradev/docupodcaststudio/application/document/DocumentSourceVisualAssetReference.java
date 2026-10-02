package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetKind;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

/** Creates canonical catalog references for materialized Word/PDF source visuals. */
final class DocumentSourceVisualAssetReference {
    private DocumentSourceVisualAssetReference() {
    }

    static ProjectAssetReference create(DocumentContentItem content,
                                        Path projectRoot,
                                        Path file,
                                        String visualFingerprint)
            throws IOException {
        Path root = projectRoot.toAbsolutePath().normalize();
        Path target = file.toAbsolutePath().normalize();
        if (!target.startsWith(root) || !Files.isRegularFile(target)) {
            throw new IOException("El visual documental no pertenece al proyecto.");
        }
        String relative = root.relativize(target).toString().replace('\\', '/');
        String id = "AST-DOCSRC-" + sha256(content.contentId() + "|"
                + visualFingerprint).substring(0, 24).toUpperCase(Locale.ROOT);
        return new ProjectAssetReference(
                id, ProjectAssetKind.STUDY_SOURCE_CROP,
                target.getFileName().toString(), relative, mimeType(target),
                "Visual original materializado del contenido documental",
                sha256(target),
                "Derivado reproducible; la fuente canonica permanece en el documento.");
    }

    private static String mimeType(Path file) {
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
        if (name.endsWith(".gif")) return "image/gif";
        if (name.endsWith(".bmp")) return "image/bmp";
        if (name.endsWith(".webp")) return "image/webp";
        return "image/png";
    }

    private static String sha256(Path file) throws IOException {
        MessageDigest digest = digest();
        try (var input = Files.newInputStream(file)) {
            byte[] buffer = new byte[64 * 1024];
            for (int read; (read = input.read(buffer)) >= 0; ) {
                if (read > 0) digest.update(buffer, 0, read);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static String sha256(String value) {
        return HexFormat.of().formatHex(digest().digest(
                (value == null ? "" : value)
                        .getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }

    private static MessageDigest digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}

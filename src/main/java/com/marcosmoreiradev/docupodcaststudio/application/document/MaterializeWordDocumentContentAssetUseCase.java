package com.marcosmoreiradev.docupodcaststudio.application.document;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;

/** Idempotently stores a Word embedded image inside the project media tree. */
public final class MaterializeWordDocumentContentAssetUseCase {
    private static final String DIRECTORY = "media/images/document-study/word-source";

    public Result materialize(DocumentContentItem content, Path projectDirectory)
            throws IOException {
        Objects.requireNonNull(content, "content");
        WordContentAnchor anchor = content.wordAnchor().orElseThrow(() ->
                new IllegalArgumentException("Word content anchor is required"));
        Map<String, String> metadata = anchor.metadata();
        String encoded = metadata.getOrDefault("embeddedImageBase64", "").strip();
        if (encoded.isBlank()) {
            throw new IOException("El elemento Word no conserva su imagen incrustada.");
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(encoded);
        } catch (IllegalArgumentException invalid) {
            throw new IOException("La imagen incrustada de Word no es Base64 valido.", invalid);
        }
        if (bytes.length == 0) throw new IOException("La imagen incrustada de Word esta vacia.");
        String visualFingerprint = sha256(bytes);
        String extension = extension(metadata.getOrDefault("embeddedImageMimeType", ""),
                metadata.getOrDefault("embeddedImagePath", ""));
        Path root = Objects.requireNonNull(projectDirectory, "projectDirectory")
                .toAbsolutePath().normalize();
        Path directory = root.resolve(DIRECTORY).normalize();
        String fileName = token(content.contentId()).toLowerCase(Locale.ROOT) + "-"
                + visualFingerprint.substring(0, 24) + extension;
        Path target = directory.resolve(fileName).normalize();
        if (!target.startsWith(directory)) {
            throw new IOException("Ruta de asset Word fuera del proyecto.");
        }
        boolean reused = Files.isRegularFile(target) && Files.size(target) == bytes.length;
        if (!reused) {
            Files.createDirectories(directory);
            Files.write(target, bytes);
        }
        ProjectAssetReference asset = DocumentSourceVisualAssetReference.create(
                content, root, target, visualFingerprint);
        return new Result(target, asset, visualFingerprint, reused);
    }

    public boolean canMaterialize(DocumentContentItem content) {
        return content != null && content.wordAnchor()
                .map(WordContentAnchor::metadata)
                .map(metadata -> !metadata.getOrDefault("embeddedImageBase64", "").isBlank())
                .orElse(false);
    }

    public record Result(Path path, ProjectAssetReference asset,
                         String visualFingerprint, boolean reused) {
        public String projectRelativePath() {
            return asset.relativePath();
        }
    }

    private static String extension(String mimeType, String sourcePath) {
        String mime = mimeType == null ? "" : mimeType.toLowerCase(Locale.ROOT);
        if (mime.contains("png")) return ".png";
        if (mime.contains("jpeg") || mime.contains("jpg")) return ".jpg";
        if (mime.contains("gif")) return ".gif";
        if (mime.contains("bmp")) return ".bmp";
        if (mime.contains("webp")) return ".webp";
        String path = sourcePath == null ? "" : sourcePath.toLowerCase(Locale.ROOT);
        for (String extension : new String[]{".png", ".jpg", ".jpeg", ".gif", ".bmp", ".webp"}) {
            if (path.endsWith(extension)) return extension;
        }
        return ".png";
    }

    private static String token(String value) {
        String result = (value == null ? "content" : value.strip())
                .replaceAll("[^A-Za-z0-9_-]", "-").replaceAll("-+", "-");
        return result.isBlank() ? "content" : result;
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}

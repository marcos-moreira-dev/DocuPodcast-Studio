package com.marcosmoreiradev.docupodcaststudio.infrastructure.theatrepackage;

import com.marcosmoreiradev.docupodcaststudio.application.theatrepackage.StagedTheatreAsset;
import com.marcosmoreiradev.docupodcaststudio.application.theatrepackage.TheatreAssetStager;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetKind;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.theatrepackage.TheatrePackageAssetKind;
import com.marcosmoreiradev.docupodcaststudio.domain.theatrepackage.TheatrePackageEntry;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Objects;

/** Non-destructive, hash-versioned materialization for package assets. */
public final class FileSystemTheatreAssetStager implements TheatreAssetStager {
    @Override
    public StagedTheatreAsset stage(Path sourceRoot, Path projectFile, Path transactionRoot,
                                    TheatrePackageEntry entry) throws IOException {
        Objects.requireNonNull(entry, "entry");
        Path root = sourceRoot.toAbsolutePath().normalize();
        Path source = root.resolve(entry.relativePath()).normalize();
        if (!source.startsWith(root) || !Files.isRegularFile(source)) {
            throw new IOException("Asset teatral fuera del origen o inexistente: " + entry.relativePath());
        }
        Path projectRoot = projectFile.toAbsolutePath().normalize().getParent();
        if (projectRoot == null) throw new IOException("El proyecto debe tener carpeta contenedora.");
        Path staging = transactionRoot.toAbsolutePath().normalize();
        if (!staging.startsWith(projectRoot)) throw new IOException("El staging debe vivir dentro del proyecto.");
        Files.createDirectories(staging);
        String extension = extension(source);
        String fileName = safe(entry.logicalId()) + "-" + entry.sha256().substring(0, 12) + "." + extension;
        String directory = directory(entry.kind());
        Path targetDirectory = projectRoot.resolve(directory).normalize();
        Path target = targetDirectory.resolve(fileName).normalize();
        if (!target.startsWith(targetDirectory)) throw new IOException("Destino teatral inseguro.");
        Path stagedFile = staging.resolve(fileName).normalize();
        Files.copy(source, stagedFile, StandardCopyOption.REPLACE_EXISTING);
        String actualHash = JsonTheatrePackageScannerHash.sha256(stagedFile);
        if (!actualHash.equals(entry.sha256())) throw new IOException("Checksum cambió durante staging: " + entry.relativePath());
        ProjectAssetReference reference = new ProjectAssetReference(assetId(entry.logicalId()), projectKind(entry.kind()),
                displayName(entry), projectRoot.relativize(target).toString().replace('\\', '/'), mimeType(extension),
                purpose(entry.kind()), "sha256:" + entry.sha256(), "Importado desde paquete teatral: " + entry.relativePath());
        return new StagedTheatreAsset(entry, reference, stagedFile, target, Files.isRegularFile(target));
    }

    @Override
    public void publish(StagedTheatreAsset staged) throws IOException {
        if (staged.targetExisted()) {
            Files.deleteIfExists(staged.stagedFile());
            return;
        }
        Files.createDirectories(staged.targetFile().getParent());
        try {
            Files.move(staged.stagedFile(), staged.targetFile(), StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(staged.stagedFile(), staged.targetFile());
        }
    }

    @Override
    public void rollbackPublished(StagedTheatreAsset staged) throws IOException {
        Files.deleteIfExists(staged.stagedFile());
        if (!staged.targetExisted()) Files.deleteIfExists(staged.targetFile());
    }

    private static String directory(TheatrePackageAssetKind kind) {
        return switch (kind) {
            case HUMAN_AUDIO, VOICE_SAMPLE -> "media/audio/theatre";
            case VIDEO -> "media/video/theatre";
            case CHARACTER_IMAGE, OBJECT_IMAGE, BACKDROP, SPATIAL_MAP, INTERVENTION_IMAGE, INTERMEDIATE_FRAME -> "media/images/theatre";
            case OTHER -> "media/theatre";
        };
    }
    private static ProjectAssetKind projectKind(TheatrePackageAssetKind kind) {
        return switch (kind) {
            case HUMAN_AUDIO -> ProjectAssetKind.AUDIO_CLIP;
            case VOICE_SAMPLE -> ProjectAssetKind.VOICE_SAMPLE;
            case VIDEO -> ProjectAssetKind.VIDEO_SOURCE;
            case CHARACTER_IMAGE, OBJECT_IMAGE, BACKDROP, SPATIAL_MAP, INTERVENTION_IMAGE, INTERMEDIATE_FRAME -> ProjectAssetKind.IMAGE;
            case OTHER -> ProjectAssetKind.OTHER;
        };
    }
    private static String purpose(TheatrePackageAssetKind kind) { return "Asset de obra teatral: " + kind.name(); }
    private static String displayName(TheatrePackageEntry entry) {
        String configured = entry.metadata("displayName");
        return configured.isBlank() ? Path.of(entry.relativePath()).getFileName().toString() : configured;
    }
    private static String assetId(String logicalId) { return "THEATRE-" + safe(logicalId).toUpperCase(Locale.ROOT); }
    private static String safe(String value) {
        String result = value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]", "-").replaceAll("-+", "-");
        return result.length() > 110 ? result.substring(0, 110) : result;
    }
    private static String extension(Path file) throws IOException {
        String name = file.getFileName().toString(); int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) throw new IOException("Asset sin extensión: " + file);
        return name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
    private static String mimeType(String extension) {
        return switch (extension) {
            case "png" -> "image/png"; case "jpg", "jpeg" -> "image/jpeg"; case "webp" -> "image/webp";
            case "wav" -> "audio/wav"; case "mp3" -> "audio/mpeg"; case "flac" -> "audio/flac";
            case "m4a" -> "audio/mp4"; case "ogg" -> "audio/ogg"; case "mp4" -> "video/mp4";
            default -> "application/octet-stream";
        };
    }

    /** Package-private bridge keeps hashing identical to scanner without exposing mutable state. */
    private static final class JsonTheatrePackageScannerHash {
        private static String sha256(Path file) throws IOException {
            try {
                var digest = java.security.MessageDigest.getInstance("SHA-256");
                try (var input = Files.newInputStream(file)) {
                    byte[] buffer = new byte[64 * 1024];
                    for (int read; (read = input.read(buffer)) >= 0;) if (read > 0) digest.update(buffer, 0, read);
                }
                return java.util.HexFormat.of().formatHex(digest.digest());
            } catch (java.security.NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
        }
    }
}

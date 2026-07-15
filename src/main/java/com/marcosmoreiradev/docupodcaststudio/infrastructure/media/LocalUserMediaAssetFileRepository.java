package com.marcosmoreiradev.docupodcaststudio.infrastructure.media;

import com.marcosmoreiradev.docupodcaststudio.application.media.UserMediaAssetRepository;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetKind;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;

/** Stores user media inside media/audio and media/video under the project container. */
public final class LocalUserMediaAssetFileRepository implements UserMediaAssetRepository {
    private static final String AUDIO_DIR = "media/audio";
    private static final String PENDING_AUDIO_DIR = "media/audio/.pending";
    private static final String VIDEO_DIR = "media/video";

    @Override
    public ProjectAssetReference importAudioClip(Path projectFile,
                                                 Path sourceAudioFile,
                                                 String assetId,
                                                 String displayName,
                                                 String purpose,
                                                 String notes) throws IOException {
        Path target = copyInto(projectFile, sourceAudioFile, AUDIO_DIR, extension(sourceAudioFile));
        return audioReference(projectFile, target, assetId,
                displayName == null || displayName.isBlank() ? fileName(sourceAudioFile) : displayName,
                purpose == null || purpose.isBlank() ? "Audio seleccionado por el usuario" : purpose,
                notes == null ? "" : notes);
    }

    @Override
    public ProjectAssetReference importVideoSource(Path projectFile,
                                                   Path sourceVideoFile,
                                                   String assetId,
                                                   String displayName,
                                                   String purpose,
                                                   String notes) throws IOException {
        Path target = copyInto(projectFile, sourceVideoFile, VIDEO_DIR, extension(sourceVideoFile));
        return new ProjectAssetReference(
                assetId,
                ProjectAssetKind.VIDEO_SOURCE,
                displayName == null || displayName.isBlank() ? fileName(sourceVideoFile) : displayName,
                relative(projectRoot(projectFile), target),
                mimeType(extension(sourceVideoFile)),
                purpose == null || purpose.isBlank() ? "Video original usado solo para extraer audio" : purpose,
                sha256(target),
                notes == null ? "" : notes
        );
    }

    @Override
    public Path prepareDerivedAudioTarget(Path projectFile, String assetId, String sourceDisplayName) throws IOException {
        Path root = projectRoot(projectFile);
        Path dir = root.resolve(AUDIO_DIR).normalize();
        if (!dir.startsWith(root)) {
            throw new IOException("Invalid media audio directory");
        }
        Files.createDirectories(dir);
        String stem = safeFileStem(assetId + "-" + stripExtension(sourceDisplayName));
        Path target = dir.resolve(stem + ".wav").normalize();
        if (!target.startsWith(dir)) {
            throw new IOException("Invalid derived audio target");
        }
        return target;
    }

    @Override
    public ProjectAssetReference registerDerivedAudioClip(Path projectFile,
                                                          Path derivedAudioFile,
                                                          String assetId,
                                                          String displayName,
                                                          String purpose,
                                                          String notes) throws IOException {
        Path root = projectRoot(projectFile);
        Path target = derivedAudioFile.toAbsolutePath().normalize();
        Path audioDir = root.resolve(AUDIO_DIR).normalize();
        if (!target.startsWith(audioDir)) {
            throw new IOException("Derived audio must be inside media/audio");
        }
        if (!Files.isRegularFile(target)) {
            throw new IOException("Derived audio file was not created: " + target);
        }
        return audioReference(projectFile, target, assetId,
                displayName == null || displayName.isBlank() ? fileName(target) : displayName,
                purpose == null || purpose.isBlank() ? "Audio derivado desde video" : purpose,
                notes == null ? "" : notes);
    }

    @Override
    public Path preparePendingAudioTarget(Path projectFile, String token, String sourceDisplayName) throws IOException {
        Path root = projectRoot(projectFile);
        Path dir = root.resolve(PENDING_AUDIO_DIR).normalize();
        if (!dir.startsWith(root)) throw new IOException("Invalid pending audio directory");
        Files.createDirectories(dir);
        Path target = dir.resolve(safeFileStem(token + "-" + stripExtension(sourceDisplayName)) + ".wav").normalize();
        if (!target.startsWith(dir)) throw new IOException("Invalid pending audio target");
        return target;
    }

    @Override
    public void copyPendingAudio(Path sourceAudioFile, Path pendingTarget) throws IOException {
        if (!Files.isRegularFile(sourceAudioFile)) throw new IOException("Audio asset not found: " + sourceAudioFile);
        Files.createDirectories(pendingTarget.toAbsolutePath().normalize().getParent());
        Files.copy(sourceAudioFile, pendingTarget, StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public Path commitPendingAudio(Path projectFile, Path pendingFile, String assetId, String displayName) throws IOException {
        Path root = projectRoot(projectFile);
        Path pendingDir = root.resolve(PENDING_AUDIO_DIR).normalize();
        Path source = pendingFile.toAbsolutePath().normalize();
        if (!source.startsWith(pendingDir) || !Files.isRegularFile(source)) {
            throw new IOException("Pending audio is missing or outside the project");
        }
        Path audioDir = root.resolve(AUDIO_DIR).normalize();
        Files.createDirectories(audioDir);
        Path target = uniqueTarget(audioDir.resolve(safeFileStem(assetId + "-" + stripExtension(displayName)) + ".wav"));
        return Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public void discardPendingAudio(Path projectFile, Path pendingFile) throws IOException {
        Path root = projectRoot(projectFile);
        Path pendingDir = root.resolve(PENDING_AUDIO_DIR).normalize();
        Path target = pendingFile.toAbsolutePath().normalize();
        if (!target.startsWith(pendingDir)) throw new IOException("Refusing to delete audio outside .pending");
        Files.deleteIfExists(target);
    }

    @Override
    public boolean deleteProjectAudioFile(Path projectFile, Path audioFile) throws IOException {
        Path root = projectRoot(projectFile);
        Path audioDir = root.resolve(AUDIO_DIR).normalize();
        Path target = audioFile.toAbsolutePath().normalize();
        if (!target.startsWith(audioDir) || target.startsWith(audioDir.resolve(".pending"))) {
            throw new IOException("Refusing to delete audio outside the committed project media directory");
        }
        return Files.deleteIfExists(target);
    }

    private ProjectAssetReference audioReference(Path projectFile,
                                                 Path target,
                                                 String assetId,
                                                 String displayName,
                                                 String purpose,
                                                 String notes) throws IOException {
        return new ProjectAssetReference(
                assetId,
                ProjectAssetKind.AUDIO_CLIP,
                displayName,
                relative(projectRoot(projectFile), target),
                mimeType(extension(target)),
                purpose,
                sha256(target),
                notes
        );
    }

    private static Path copyInto(Path projectFile, Path sourceFile, String relativeDirectory, String extension) throws IOException {
        Objects.requireNonNull(sourceFile, "sourceFile");
        if (!Files.isRegularFile(sourceFile)) {
            throw new IOException("Media asset not found: " + sourceFile);
        }
        Path root = projectRoot(projectFile);
        Path dir = root.resolve(relativeDirectory).normalize();
        if (!dir.startsWith(root)) {
            throw new IOException("Invalid media target directory");
        }
        Files.createDirectories(dir);
        String fileName = safeFileStem(stripExtension(sourceFile.getFileName().toString())) + "." + extension;
        Path target = uniqueTarget(dir.resolve(fileName));
        Files.copy(sourceFile, target, StandardCopyOption.REPLACE_EXISTING);
        return target;
    }

    private static Path uniqueTarget(Path desired) {
        if (!Files.exists(desired)) {
            return desired;
        }
        String name = desired.getFileName().toString();
        int dot = name.lastIndexOf('.');
        String base = dot < 0 ? name : name.substring(0, dot);
        String extension = dot < 0 ? "" : name.substring(dot);
        Path parent = desired.getParent();
        int index = 2;
        Path candidate;
        do {
            candidate = parent.resolve(base + "-" + index++ + extension);
        } while (Files.exists(candidate));
        return candidate;
    }

    private static Path projectRoot(Path projectFile) throws IOException {
        Objects.requireNonNull(projectFile, "projectFile");
        Path root = projectFile.toAbsolutePath().normalize().getParent();
        if (root == null) {
            throw new IOException("Project file must have a parent directory");
        }
        return root;
    }

    private static String relative(Path root, Path target) {
        return root.relativize(target.toAbsolutePath().normalize()).toString().replace('\\', '/');
    }

    private static String extension(Path path) throws IOException {
        String name = fileName(path);
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            throw new IOException("Media file must have an extension");
        }
        return name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static String stripExtension(String name) {
        int dot = name == null ? -1 : name.lastIndexOf('.');
        return dot < 0 ? Objects.toString(name, "media") : name.substring(0, dot);
    }

    private static String fileName(Path path) {
        return path == null || path.getFileName() == null ? "media" : path.getFileName().toString();
    }

    private static String safeFileStem(String raw) {
        String normalized = raw == null ? "media" : raw.strip().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9._-]", "-")
                .replaceAll("-+", "-");
        if (normalized.isBlank() || normalized.equals("-") || normalized.equals(".")) {
            return "media";
        }
        return normalized.length() > 96 ? normalized.substring(0, 96) : normalized;
    }

    private static String mimeType(String extension) {
        return switch (extension == null ? "" : extension.toLowerCase(Locale.ROOT)) {
            case "wav" -> "audio/wav";
            case "mp3" -> "audio/mpeg";
            case "m4a" -> "audio/mp4";
            case "flac" -> "audio/flac";
            case "ogg" -> "audio/ogg";
            case "mp4" -> "video/mp4";
            case "mov" -> "video/quicktime";
            case "mkv" -> "video/x-matroska";
            case "webm" -> "video/webm";
            default -> "application/octet-stream";
        };
    }

    private static String sha256(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = Files.newInputStream(file)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) >= 0) {
                    digest.update(buffer, 0, read);
                }
            }
            return "sha256:" + HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException ex) {
            throw new IOException("SHA-256 not available", ex);
        }
    }
}

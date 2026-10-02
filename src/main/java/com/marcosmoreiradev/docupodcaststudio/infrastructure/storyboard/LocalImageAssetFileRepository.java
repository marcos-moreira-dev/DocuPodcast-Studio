package com.marcosmoreiradev.docupodcaststudio.infrastructure.storyboard;

import com.marcosmoreiradev.docupodcaststudio.application.storyboard.ImageAssetRepository;
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

/** Copies user-selected images into media/images/ beside the project file. */
public final class LocalImageAssetFileRepository implements ImageAssetRepository {
    private static final String IMAGE_DIR = "media/images";

    @Override
    public ProjectAssetReference importImage(Path projectFile,
                                             Path sourceImageFile,
                                             String assetId,
                                             String displayName,
                                             String purpose,
                                             String notes) throws IOException {
        Objects.requireNonNull(projectFile, "projectFile");
        Objects.requireNonNull(sourceImageFile, "sourceImageFile");
        Path projectDirectory = projectFile.toAbsolutePath().normalize().getParent();
        if (projectDirectory == null) {
            throw new IOException("Project file must have a parent directory");
        }
        if (!Files.isRegularFile(sourceImageFile)) {
            throw new IOException("Image asset not found: " + sourceImageFile);
        }
        String extension = extension(sourceImageFile);
        if (!java.util.Set.of("png", "jpg", "jpeg", "webp", "gif").contains(extension)) {
            throw new IOException("El archivo no tiene un formato de imagen admitido: " + extension);
        }
        String fileName = safeFileStem(assetId + "-" + stripExtension(sourceImageFile.getFileName().toString())) + "." + extension;
        Path target = projectDirectory.resolve(IMAGE_DIR).resolve(fileName).normalize();
        if (!target.startsWith(projectDirectory.resolve(IMAGE_DIR).normalize())) {
            throw new IOException("Invalid target image path");
        }
        Files.createDirectories(target.getParent());
        Files.copy(sourceImageFile, target, StandardCopyOption.REPLACE_EXISTING);
        String relativePath = IMAGE_DIR + "/" + fileName;
        return new ProjectAssetReference(
                assetId,
                ProjectAssetKind.IMAGE,
                displayName == null || displayName.isBlank() ? sourceImageFile.getFileName().toString() : displayName,
                relativePath,
                mimeType(extension),
                purpose == null ? "Imagen de storyboard" : purpose,
                sha256(target),
                notes == null ? "" : notes
        );
    }

    private static String extension(Path path) throws IOException {
        String name = path.getFileName() == null ? "" : path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            throw new IOException("Image file must have an extension");
        }
        return name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static String stripExtension(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? name : name.substring(0, dot);
    }

    private static String safeFileStem(String raw) {
        String normalized = raw == null ? "storyboard-image" : raw.strip().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9._-]", "-")
                .replaceAll("-+", "-");
        if (normalized.isBlank() || normalized.equals("-") || normalized.equals(".")) {
            return "storyboard-image";
        }
        return normalized.length() > 96 ? normalized.substring(0, 96) : normalized;
    }

    private static String mimeType(String extension) {
        return switch (extension) {
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "webp" -> "image/webp";
            case "gif" -> "image/gif";
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

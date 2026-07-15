package com.marcosmoreiradev.docupodcaststudio.infrastructure.voice;

import com.marcosmoreiradev.docupodcaststudio.application.voice.VoiceSampleRepository;
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

/**
 * Copies user-selected voice samples into a managed DocuPodcast voice library.
 *
 * <p>The default constructor keeps the legacy project-local layout for focused tests. Product wiring uses the
 * application-root constructor so user voice references live in {@code voice-library/samples/} beside the app/runtime,
 * not inside each individual project folder.</p>
 */
public final class LocalVoiceSampleFileRepository implements VoiceSampleRepository {
    private static final String LEGACY_SAMPLE_DIR = "voices/samples";
    private static final String APP_VOICE_LIBRARY_DIR = "voice-library";
    private static final String APP_SAMPLE_DIR = "voice-library/samples";

    private final Path applicationRoot;

    public LocalVoiceSampleFileRepository() {
        this.applicationRoot = null;
    }

    public LocalVoiceSampleFileRepository(Path applicationRoot) {
        this.applicationRoot = Objects.requireNonNull(applicationRoot, "applicationRoot").toAbsolutePath().normalize();
    }

    @Override
    public boolean storesSamplesOutsideProject() {
        return applicationRoot != null;
    }

    @Override
    public Path effectiveProjectFile(Path projectFile) throws IOException {
        if (applicationRoot != null) {
            Path libraryDir = applicationRoot.resolve(APP_VOICE_LIBRARY_DIR).normalize();
            Files.createDirectories(libraryDir);
            return libraryDir.resolve("voice-library.docupodcast.json").normalize();
        }
        return VoiceSampleRepository.super.effectiveProjectFile(projectFile);
    }

    @Override
    public Path temporaryRecordingDirectory(Path projectFile) throws IOException {
        if (applicationRoot != null) {
            Path dir = applicationRoot.resolve(APP_VOICE_LIBRARY_DIR).resolve("tmp-recordings").normalize();
            if (!dir.startsWith(applicationRoot.resolve(APP_VOICE_LIBRARY_DIR).normalize())) {
                throw new IOException("Invalid application voice recording directory");
            }
            Files.createDirectories(dir);
            return dir;
        }
        return VoiceSampleRepository.super.temporaryRecordingDirectory(projectFile);
    }

    @Override
    public ProjectAssetReference importSample(Path projectFile,
                                              Path sourceAudioFile,
                                              String assetId,
                                              String displayName,
                                              String purpose,
                                              String notes) throws IOException {
        Objects.requireNonNull(sourceAudioFile, "sourceAudioFile");
        Path projectDirectory = projectDirectory(effectiveProjectFile(projectFile));
        if (!Files.isRegularFile(sourceAudioFile)) {
            throw new IOException("Voice sample not found: " + sourceAudioFile);
        }
        String extension = extension(sourceAudioFile);
        String fileName = safeFileStem(assetId + "-" + stripExtension(sourceAudioFile.getFileName().toString())) + "." + extension;
        Path storageRoot = sampleStorageRoot(projectDirectory);
        Path target = storageRoot.resolve(fileName).normalize();
        if (!target.startsWith(storageRoot)) {
            throw new IOException("Invalid target voice sample path");
        }
        Files.createDirectories(target.getParent());
        Files.copy(sourceAudioFile, target, StandardCopyOption.REPLACE_EXISTING);
        String relativePath = relativePathForManagedTarget(projectDirectory, target);
        return new ProjectAssetReference(
                assetId,
                ProjectAssetKind.VOICE_SAMPLE,
                displayName == null || displayName.isBlank() ? sourceAudioFile.getFileName().toString() : displayName,
                relativePath,
                mimeType(extension),
                purpose == null ? "Muestra de voz" : purpose,
                sha256(target),
                notes == null ? "" : notes
        );
    }

    @Override
    public String referenceUriForImportedSample(Path projectFile, ProjectAssetReference sampleAsset) throws IOException {
        Path resolved = resolveManagedSample(projectFile, sampleAsset);
        return resolved.toAbsolutePath().normalize().toString();
    }

    @Override
    public Path resolveManagedSample(Path projectFile, ProjectAssetReference sampleAsset) throws IOException {
        Objects.requireNonNull(sampleAsset, "sampleAsset");
        Path projectDirectory = projectDirectory(effectiveProjectFile(projectFile));
        Path managedRoot = sampleStorageRoot(projectDirectory);
        Path resolved = samplePath(projectDirectory, sampleAsset.relativePath()).normalize();
        if (!resolved.startsWith(managedRoot)) {
            throw new IOException("Voice sample is not inside the managed voice samples folder");
        }
        return resolved;
    }

    @Override
    public Path downloadSample(Path projectFile,
                               ProjectAssetReference sampleAsset,
                               Path targetDirectory,
                               String preferredFileName) throws IOException {
        Objects.requireNonNull(targetDirectory, "targetDirectory");
        if (!Files.isDirectory(targetDirectory)) {
            throw new IOException("Target directory does not exist: " + targetDirectory);
        }
        Path source = resolveManagedSample(projectFile, sampleAsset);
        if (!Files.isRegularFile(source)) {
            throw new IOException("Managed voice sample not found: " + source);
        }
        String fileName = preferredFileName == null || preferredFileName.isBlank()
                ? source.getFileName().toString()
                : safeDownloadFileName(preferredFileName, source);
        Path target = uniqueTarget(targetDirectory.toAbsolutePath().normalize(), fileName);
        Files.copy(source, target, StandardCopyOption.COPY_ATTRIBUTES);
        return target;
    }

    @Override
    public boolean deleteManagedSample(Path projectFile, ProjectAssetReference sampleAsset) throws IOException {
        Path source = resolveManagedSample(projectFile, sampleAsset);
        if (!Files.exists(source)) {
            return false;
        }
        Files.delete(source);
        return true;
    }

    private Path sampleStorageRoot(Path projectDirectory) {
        return applicationRoot == null
                ? projectDirectory.resolve(LEGACY_SAMPLE_DIR).normalize()
                : applicationRoot.resolve(APP_SAMPLE_DIR).normalize();
    }

    private Path samplePath(Path projectDirectory, String storedRelativePath) {
        String normalized = storedRelativePath == null ? "" : storedRelativePath.replace('\\', '/').strip();
        if (applicationRoot != null && normalized.startsWith(APP_SAMPLE_DIR + "/")) {
            return applicationRoot.resolve(normalized).normalize();
        }
        return projectDirectory.resolve(normalized).normalize();
    }

    private String relativePathForManagedTarget(Path projectDirectory, Path target) {
        Path normalizedTarget = target.toAbsolutePath().normalize();
        if (applicationRoot != null) {
            return applicationRoot.relativize(normalizedTarget).toString().replace('\\', '/');
        }
        return projectDirectory.relativize(normalizedTarget).toString().replace('\\', '/');
    }

    private static Path projectDirectory(Path projectFile) throws IOException {
        Path projectDirectory = projectFile.toAbsolutePath().normalize().getParent();
        if (projectDirectory == null) {
            throw new IOException("Project file must have a parent directory");
        }
        return projectDirectory;
    }

    private static String safeDownloadFileName(String preferredFileName, Path source) throws IOException {
        String raw = preferredFileName == null ? "" : preferredFileName.strip();
        if (raw.isBlank()) {
            return source.getFileName().toString();
        }
        String sourceExtension = extension(source);
        String cleaned = safeFileStem(stripExtension(raw));
        if (cleaned.isBlank()) {
            cleaned = safeFileStem(stripExtension(source.getFileName().toString()));
        }
        String preferredExtension = "";
        int dot = raw.lastIndexOf('.');
        if (dot >= 0 && dot < raw.length() - 1) {
            preferredExtension = raw.substring(dot + 1).toLowerCase(Locale.ROOT);
        }
        String extension = preferredExtension.isBlank() ? sourceExtension : preferredExtension;
        return cleaned + "." + extension;
    }

    private static Path uniqueTarget(Path targetDirectory, String fileName) throws IOException {
        Files.createDirectories(targetDirectory);
        Path candidate = targetDirectory.resolve(fileName).normalize();
        if (!candidate.startsWith(targetDirectory)) {
            throw new IOException("Invalid download target path");
        }
        if (!Files.exists(candidate)) {
            return candidate;
        }
        String stem = stripExtension(fileName);
        String extension = extension(Path.of(fileName));
        for (int i = 2; i < 10_000; i++) {
            Path numbered = targetDirectory.resolve(stem + "-" + i + "." + extension).normalize();
            if (!numbered.startsWith(targetDirectory)) {
                throw new IOException("Invalid download target path");
            }
            if (!Files.exists(numbered)) {
                return numbered;
            }
        }
        throw new IOException("Could not create a unique download filename for " + fileName);
    }

    private static String extension(Path path) throws IOException {
        String name = path.getFileName() == null ? "" : path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            throw new IOException("Voice sample file must have an audio extension");
        }
        return name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static String stripExtension(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? name : name.substring(0, dot);
    }

    private static String safeFileStem(String raw) {
        String normalized = raw == null ? "voice-sample" : raw.strip().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9._-]", "-")
                .replaceAll("-+", "-");
        if (normalized.isBlank() || normalized.equals("-") || normalized.equals(".")) {
            return "voice-sample";
        }
        return normalized.length() > 96 ? normalized.substring(0, 96) : normalized;
    }

    private static String mimeType(String extension) {
        return switch (extension) {
            case "wav" -> "audio/wav";
            case "mp3" -> "audio/mpeg";
            case "flac" -> "audio/flac";
            case "ogg" -> "audio/ogg";
            case "m4a" -> "audio/mp4";
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

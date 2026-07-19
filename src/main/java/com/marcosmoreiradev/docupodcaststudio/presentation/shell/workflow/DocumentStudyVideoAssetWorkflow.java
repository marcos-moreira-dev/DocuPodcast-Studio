package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.application.ApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.application.ink.InkPlacedImage;
import com.marcosmoreiradev.docupodcaststudio.application.ink.InkWorkspaceState;
import com.marcosmoreiradev.docupodcaststudio.application.ink.InkWorkspaceStateSerializer;
import com.marcosmoreiradev.docupodcaststudio.application.media.PreparedAudioAsset;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetKind;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentStudyMusicTrack;
import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentStudyVideoConfiguration;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.ProjectSession;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Owns project-side imports and files used by documentary video configuration. */
public final class DocumentStudyVideoAssetWorkflow {
    public void update(ProjectSession session, DocumentStudyVideoConfiguration configuration) {
        Objects.requireNonNull(session, "session");
        session.replaceProject(session.project().withStudy(
                session.project().study().withDocumentaryVideoConfiguration(configuration)), true);
    }

    public ProjectAssetReference importImage(ApplicationServices services, ProjectSession session, Path sourceFile)
            throws IOException {
        Path projectFile = requireProjectFile(session, "importar imagenes documentales");
        var result = services.storyboard().importImageAsset().importImage(session.project(), projectFile, sourceFile);
        session.replaceProject(result.project(), true);
        return result.imageAsset();
    }

    public DrawingAsset saveDrawing(ProjectSession session, String blockId, Path temporaryPng,
                                    String inkStateJson) throws IOException {
        return saveDrawing(session, blockId, temporaryPng, inkStateJson, Map.of());
    }

    public DrawingAsset saveDrawing(ProjectSession session, String blockId, Path temporaryPng,
                                    String inkStateJson, Map<String, Path> stagedSources) throws IOException {
        Path projectFile = requireProjectFile(session, "dibujar imagenes documentales");
        Path root = projectFile.toAbsolutePath().normalize().getParent();
        String safeBlock = blockId == null ? "paragraph" : blockId.replaceAll("[^A-Za-z0-9_-]", "-");
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Path directory = root.resolve("media/images/document-study/drawings");
        Path png = directory.resolve(safeBlock + "-" + suffix + ".png");
        Path sidecar = directory.resolve(safeBlock + "-" + suffix + ".ink.json");
        Path sourceDirectory = root.resolve("media/images/document-study/illustrations/sources");
        Path staging = root.resolve("media/images/document-study/.illustration-" + UUID.randomUUID());
        Files.createDirectories(staging);
        List<Path> committed = new ArrayList<>();
        List<ProjectAssetReference> sourceAssets = new ArrayList<>();
        Map<String, ProjectAssetReference> sourceByImageId = new LinkedHashMap<>();
        try {
            for (Map.Entry<String, Path> entry : safeSources(stagedSources).entrySet()) {
                Path source = entry.getValue();
                if (source == null || !Files.isRegularFile(source)) {
                    throw new IOException("No se encontro una imagen fuente de la ilustracion.");
                }
                String extension = extension(source);
                Path target = sourceDirectory.resolve(safeBlock + "-" + entry.getKey().replaceAll("[^A-Za-z0-9_-]", "-")
                        + "-" + suffix + extension);
                Path staged = staging.resolve(target.getFileName());
                Files.copy(source, staged, StandardCopyOption.REPLACE_EXISTING);
                ProjectAssetReference asset = new ProjectAssetReference(
                        "DOC-ILL-SOURCE-" + UUID.randomUUID().toString().replace("-", ""),
                        ProjectAssetKind.IMAGE, "Fuente de ilustracion " + safeBlock,
                        portable(root, target), imageMime(target),
                        "Fuente colocada en una ilustracion documental", "", "Bloque Word " + safeBlock);
                sourceAssets.add(asset);
                sourceByImageId.put(entry.getKey(), asset);
            }

            String persistedState = persistedState(inkStateJson, sourceByImageId);
            Path stagedPng = staging.resolve(png.getFileName());
            Path stagedSidecar = staging.resolve(sidecar.getFileName());
            Files.copy(temporaryPng, stagedPng, StandardCopyOption.REPLACE_EXISTING);
            Files.writeString(stagedSidecar, persistedState, StandardCharsets.UTF_8);

            Files.createDirectories(directory);
            Files.createDirectories(sourceDirectory);
            for (ProjectAssetReference sourceAsset : sourceAssets) {
                Path target = root.resolve(sourceAsset.relativePath()).normalize();
                move(staging.resolve(target.getFileName()), target);
                committed.add(target);
            }
            move(stagedPng, png);
            committed.add(png);
            move(stagedSidecar, sidecar);
            committed.add(sidecar);

            ProjectAssetReference asset = drawingAsset(root, safeBlock, png);
            DocuPodcastProject updated = session.project();
            for (ProjectAssetReference sourceAsset : sourceAssets) updated = updated.withAsset(sourceAsset);
            updated = updated.withAsset(asset);
            session.replaceProject(updated, true);
            return new DrawingAsset(asset, portable(root, sidecar));
        } catch (IOException | RuntimeException ex) {
            for (Path file : committed) {
                try { Files.deleteIfExists(file); } catch (IOException ignored) { }
            }
            throw ex;
        } finally {
            deleteTree(staging);
        }
    }

    private static ProjectAssetReference drawingAsset(Path root, String safeBlock, Path png) {
        ProjectAssetReference asset = new ProjectAssetReference(
                "DOC-DRAW-" + UUID.randomUUID().toString().replace("-", ""),
                ProjectAssetKind.IMAGE, "Dibujo " + safeBlock, portable(root, png), "image/png",
                "Dibujo para video documental", "", "Bloque Word " + safeBlock);
        return asset;
    }

    private static String persistedState(String json, Map<String, ProjectAssetReference> sourceByImageId)
            throws IOException {
        String safeJson = json == null || json.isBlank() ? "{}" : json;
        if (sourceByImageId.isEmpty()) return safeJson;
        InkWorkspaceState state = InkWorkspaceStateSerializer.fromJson(safeJson);
        List<InkPlacedImage> images = state.images().stream().map(image -> {
            ProjectAssetReference source = sourceByImageId.get(image.id());
            if (source == null) return image;
            return new InkPlacedImage(image.id(), source.id(), source.relativePath(), image.inlineImageData(),
                    image.inlineOriginalImageData(), image.x(), image.y(), image.fitWidth(), image.height(),
                    image.originalLayoutX(), image.originalLayoutY(), image.originalFitWidth(), image.crop());
        }).toList();
        return InkWorkspaceStateSerializer.toJson(InkWorkspaceState.create(state.logicalWidth(), state.logicalHeight(),
                state.background(), state.strokes(), images, state.metadata()));
    }

    private static Map<String, Path> safeSources(Map<String, Path> sources) {
        return sources == null ? Map.of() : Map.copyOf(sources);
    }

    private static String extension(Path source) {
        String name = source.getFileName() == null ? "" : source.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot >= 0 && dot < name.length() - 1 ? name.substring(dot).toLowerCase() : ".png";
    }

    private static String imageMime(Path file) {
        try {
            String detected = Files.probeContentType(file);
            if (detected != null && detected.startsWith("image/")) return detected;
        } catch (IOException ignored) { }
        return switch (extension(file)) {
            case ".jpg", ".jpeg" -> "image/jpeg";
            case ".webp" -> "image/webp";
            case ".bmp" -> "image/bmp";
            default -> "image/png";
        };
    }

    private static void move(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException ex) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void deleteTree(Path directory) {
        if (directory == null || !Files.exists(directory)) return;
        try (var files = Files.walk(directory)) {
            files.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                try { Files.deleteIfExists(path); } catch (IOException ignored) { }
            });
        } catch (IOException ignored) { }
    }

    public MusicImport importMusic(ApplicationServices services, ProjectSession session, Path sourceFile)
            throws IOException {
        Path projectFile = requireProjectFile(session, "importar musica documental");
        var importer = services.media().importUserMediaAsset();
        PreparedAudioAsset prepared = importer.prepareAudio(projectFile, sourceFile);
        try {
            var result = importer.commitPreparedAudio(session.project(), projectFile, prepared);
            session.replaceProject(result.project(), true);
            DocumentStudyMusicTrack track = new DocumentStudyMusicTrack(
                    "DOC-MUSIC-" + UUID.randomUUID().toString().replace("-", ""),
                    result.audioAsset().id(), prepared.durationSeconds(), 0.20);
            return new MusicImport(track, result.audioAsset().displayName());
        } catch (IOException | RuntimeException ex) {
            importer.discardPreparedAudio(projectFile, prepared);
            throw ex;
        }
    }

    public Optional<Path> resolveAsset(Optional<DocuPodcastProject> project, Optional<Path> projectFile,
                                       String assetId) {
        if (assetId == null || assetId.isBlank() || project.isEmpty() || projectFile.isEmpty()) return Optional.empty();
        return project.get().assets().byId(assetId).flatMap(asset -> resolveRelative(projectFile, asset.relativePath()));
    }

    public Optional<Path> resolveRelative(Optional<Path> projectFile, String relativePath) {
        if (relativePath == null || relativePath.isBlank() || projectFile.isEmpty()
                || projectFile.get().getParent() == null) return Optional.empty();
        Path root = projectFile.get().toAbsolutePath().normalize().getParent();
        Path resolved = root.resolve(relativePath).toAbsolutePath().normalize();
        return resolved.startsWith(root) && Files.isRegularFile(resolved) ? Optional.of(resolved) : Optional.empty();
    }

    private static Path requireProjectFile(ProjectSession session, String action) throws IOException {
        return Objects.requireNonNull(session, "session").projectFile()
                .orElseThrow(() -> new IOException("Guarda el proyecto antes de " + action + "."));
    }

    private static String portable(Path root, Path file) {
        return root.relativize(file).toString().replace('\\', '/');
    }

    public record DrawingAsset(ProjectAssetReference asset, String stateRelativePath) { }
    public record MusicImport(DocumentStudyMusicTrack track, String displayName) { }
}

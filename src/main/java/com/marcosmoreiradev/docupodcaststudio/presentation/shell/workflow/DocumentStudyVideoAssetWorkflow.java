package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.application.ApplicationServices;
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
        Path projectFile = requireProjectFile(session, "dibujar imagenes documentales");
        Path root = projectFile.toAbsolutePath().normalize().getParent();
        String safeBlock = blockId == null ? "paragraph" : blockId.replaceAll("[^A-Za-z0-9_-]", "-");
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Path directory = root.resolve("media/images/document-study/drawings");
        Files.createDirectories(directory);
        Path png = directory.resolve(safeBlock + "-" + suffix + ".png");
        Path sidecar = directory.resolve(safeBlock + "-" + suffix + ".ink.json");
        Files.copy(temporaryPng, png, StandardCopyOption.REPLACE_EXISTING);
        Files.writeString(sidecar, inkStateJson == null ? "{}" : inkStateJson, StandardCharsets.UTF_8);
        ProjectAssetReference asset = new ProjectAssetReference(
                "DOC-DRAW-" + UUID.randomUUID().toString().replace("-", ""),
                ProjectAssetKind.IMAGE, "Dibujo " + safeBlock, portable(root, png), "image/png",
                "Dibujo para video documental", "", "Bloque Word " + safeBlock);
        session.replaceProject(session.project().withAsset(asset), true);
        return new DrawingAsset(asset, portable(root, sidecar));
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

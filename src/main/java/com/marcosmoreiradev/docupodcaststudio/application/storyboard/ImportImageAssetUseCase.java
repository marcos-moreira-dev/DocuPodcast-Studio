package com.marcosmoreiradev.docupodcaststudio.application.storyboard;

import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;

/** Imports an image into media/images/ and registers it as a portable project asset. */
public final class ImportImageAssetUseCase {
    private final ImageAssetRepository repository;

    public ImportImageAssetUseCase(ImageAssetRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public ImageAssetImportResult importImage(DocuPodcastProject project, Path projectFile, Path sourceImageFile) throws IOException {
        Objects.requireNonNull(project, "project");
        Objects.requireNonNull(projectFile, "projectFile");
        Objects.requireNonNull(sourceImageFile, "sourceImageFile");
        if (projectFile.getParent() == null) {
            throw new IOException("Guarda el proyecto en una carpeta antes de importar imágenes");
        }
        if (!Files.isRegularFile(sourceImageFile)) {
            throw new IOException("La imagen no existe o no es un archivo: " + sourceImageFile);
        }
        String extension = extension(sourceImageFile);
        if (!supported(extension)) {
            throw new IOException("Formato de imagen no soportado: ." + extension);
        }
        String assetId = "IMG-" + String.format(java.util.Locale.ROOT, "%03d", project.assets().byKind(com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetKind.IMAGE).size() + 1);
        String displayName = sourceImageFile.getFileName() == null ? assetId : sourceImageFile.getFileName().toString();
        ProjectAssetReference asset = repository.importImage(projectFile, sourceImageFile, assetId, displayName,
                "Imagen aportada por el usuario para storyboard vivo", "");
        DocuPodcastProject updated = project.withAsset(asset);
        return new ImageAssetImportResult(updated, asset);
    }

    private static String extension(Path file) throws IOException {
        String name = file.getFileName() == null ? "" : file.getFileName().toString();
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            throw new IOException("La imagen debe tener extensión");
        }
        return name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static boolean supported(String extension) {
        return switch (extension) {
            case "png", "jpg", "jpeg", "webp", "gif" -> true;
            default -> false;
        };
    }
}

package com.marcosmoreiradev.docupodcaststudio.application.examples;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Copies bundled demo resources to the chosen project folder before normal import/save. */
public final class CreateExampleProjectUseCase {
    public ExampleProjectMaterialization materialize(ExampleProjectDescriptor example, Path projectFile) throws IOException {
        Objects.requireNonNull(example, "example");
        Objects.requireNonNull(projectFile, "projectFile");
        Path normalizedProjectFile = projectFile.toAbsolutePath().normalize();
        Path projectDirectory = normalizedProjectFile.getParent();
        if (projectDirectory == null) {
            throw new IOException("El proyecto demo necesita una carpeta contenedora.");
        }
        Path stagingDirectory = projectDirectory.resolve("examples").resolve(example.id()).normalize();
        if (!stagingDirectory.startsWith(projectDirectory)) {
            throw new IOException("Ruta de ejemplo invalida.");
        }
        Files.createDirectories(stagingDirectory.resolve("source"));
        Files.createDirectories(stagingDirectory.resolve("assets"));

        Path sourceDocument = stagingDirectory.resolve("source").resolve(example.sourceDocumentFileName()).normalize();
        copyResource(example.sourceDocumentResource(), sourceDocument);

        List<Path> copiedAssets = new ArrayList<>();
        for (ExampleAssetDescriptor asset : example.assets()) {
            Path target = stagingDirectory.resolve("assets").resolve(asset.fileName()).normalize();
            copyResource(asset.resourcePath(), target);
            copiedAssets.add(target);
        }
        Path theatreMarkdownFile = null;
        if (example.hasTheatreMarkdown()) {
            theatreMarkdownFile = stagingDirectory.resolve("teatro.md").normalize();
            copyResource(example.theatreMarkdownResource(), theatreMarkdownFile);
        }
        return new ExampleProjectMaterialization(example, normalizedProjectFile, sourceDocument, copiedAssets, theatreMarkdownFile);
    }

    public ExampleProjectMaterialization materializeInDirectory(ExampleProjectDescriptor example, Path parentDirectory) throws IOException {
        Objects.requireNonNull(example, "example");
        Objects.requireNonNull(parentDirectory, "parentDirectory");
        Path normalizedParent = parentDirectory.toAbsolutePath().normalize();
        Path projectDirectory = normalizedParent.resolve(safeProjectFolderName(example.defaultProjectName())).normalize();
        if (!projectDirectory.startsWith(normalizedParent)) {
            throw new IOException("Ruta de ejemplo invalida.");
        }
        Files.createDirectories(projectDirectory);
        Path sourceDirectory = projectDirectory.resolve("source").normalize();
        Path assetsDirectory = projectDirectory.resolve("assets").normalize();
        if (!sourceDirectory.startsWith(projectDirectory) || !assetsDirectory.startsWith(projectDirectory)) {
            throw new IOException("Ruta de ejemplo invalida.");
        }
        Files.createDirectories(sourceDirectory);
        Files.createDirectories(assetsDirectory);

        Path sourceDocument = sourceDirectory.resolve("source.docx").normalize();
        copyResource(example.sourceDocumentResource(), sourceDocument);

        List<Path> copiedAssets = new ArrayList<>();
        for (ExampleAssetDescriptor asset : example.assets()) {
            Path target = assetsDirectory.resolve(asset.fileName()).normalize();
            if (!target.startsWith(assetsDirectory)) {
                throw new IOException("Asset de ejemplo invalido: " + asset.fileName());
            }
            copyResource(asset.resourcePath(), target);
            copiedAssets.add(target);
        }

        Path theatreMarkdownFile = null;
        if (example.hasTheatreMarkdown()) {
            theatreMarkdownFile = projectDirectory.resolve("teatro.md").normalize();
            copyResource(example.theatreMarkdownResource(), theatreMarkdownFile);
        }

        Path projectFile = projectDirectory.resolve(safeProjectFileName(example.defaultProjectName())).normalize();
        return new ExampleProjectMaterialization(example, projectFile, sourceDocument, copiedAssets, theatreMarkdownFile);
    }

    private static String safeProjectFolderName(String displayName) {
        String base = displayName == null ? "" : displayName.strip()
                .replaceAll("\\.docupodcast\\.json$", "")
                .replaceAll("[\\\\/:*?\"<>|]+", " ")
                .replaceAll("\\s+", " ")
                .strip();
        return base.isBlank() ? "Proyecto Demo" : base;
    }

    private static String safeProjectFileName(String displayName) {
        String base = displayName == null ? "" : displayName.strip()
                .replaceAll("[\\\\/:*?\"<>|]+", " ")
                .replaceAll("\\s+", " ")
                .strip();
        if (base.isBlank()) {
            base = "Proyecto Demo";
        }
        if (!base.toLowerCase(java.util.Locale.ROOT).endsWith(".docupodcast.json")) {
            base += ".docupodcast.json";
        }
        return base;
    }

    private static void copyResource(String resourcePath, Path target) throws IOException {
        String normalizedResource = resourcePath.startsWith("/") ? resourcePath : "/" + resourcePath;
        try (InputStream input = CreateExampleProjectUseCase.class.getResourceAsStream(normalizedResource)) {
            if (input == null) {
                throw new IOException("Recurso de ejemplo no encontrado: " + resourcePath);
            }
            Files.createDirectories(target.getParent());
            Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}

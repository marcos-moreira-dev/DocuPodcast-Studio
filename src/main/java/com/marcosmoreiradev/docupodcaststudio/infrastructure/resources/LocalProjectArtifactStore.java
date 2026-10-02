package com.marcosmoreiradev.docupodcaststudio.infrastructure.resources;

import com.marcosmoreiradev.docupodcaststudio.application.artifacts.ProjectArtifactStore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/** Local filesystem implementation with project-root confinement. */
public final class LocalProjectArtifactStore implements ProjectArtifactStore {
    @Override
    public Path projectRoot(Path projectFile) throws IOException {
        Path normalized = Objects.requireNonNull(projectFile, "project file").toAbsolutePath().normalize();
        Path root = normalized.getParent();
        if (root == null) {
            throw new IOException("El proyecto no tiene una carpeta contenedora valida.");
        }
        return root;
    }

    @Override
    public Path resolveExisting(Path projectFile, String relativePath) throws IOException {
        if (relativePath == null || relativePath.isBlank()) {
            throw new IOException("El recurso del proyecto no tiene una ruta relativa valida.");
        }
        Path root = projectRoot(projectFile);
        Path resolved = root.resolve(relativePath).toAbsolutePath().normalize();
        if (!resolved.startsWith(root) || !Files.isRegularFile(resolved)) {
            throw new IOException("El recurso no existe dentro de la carpeta del proyecto.");
        }
        return resolved;
    }

    @Override
    public boolean isProjectOwnedRegularFile(Path projectFile, Path candidate) throws IOException {
        if (candidate == null) {
            return false;
        }
        Path root = projectRoot(projectFile);
        Path normalized = candidate.toAbsolutePath().normalize();
        return normalized.startsWith(root) && Files.isRegularFile(normalized);
    }

    @Override
    public void discardProjectOwned(Path projectFile, Path candidate) throws IOException {
        if (candidate == null) {
            return;
        }
        Path root = projectRoot(projectFile);
        Path normalized = candidate.toAbsolutePath().normalize();
        if (!normalized.startsWith(root)) {
            throw new IOException("No se puede descartar un artefacto fuera del proyecto.");
        }
        Files.deleteIfExists(normalized);
    }
}

package com.marcosmoreiradev.docupodcaststudio.application.visual;

import java.nio.file.Path;

/** Consumer-provided output policy that keeps generated assets in the intended project. */
public record VisualOutputTarget(
        Path projectRoot,
        Path relativeDirectory,
        String filenamePrefix,
        boolean requireInsideProject
) {
    public VisualOutputTarget {
        projectRoot = projectRoot == null ? null : projectRoot.toAbsolutePath().normalize();
        relativeDirectory = relativeDirectory == null ? Path.of("generated/visual") : relativeDirectory.normalize();
        filenamePrefix = safe(filenamePrefix);
        if (filenamePrefix.isBlank()) {
            filenamePrefix = "docupodcast-visual";
        }
        if (relativeDirectory.isAbsolute()) {
            throw new IllegalArgumentException("El directorio visual debe ser relativo al proyecto.");
        }
        if (requireInsideProject && projectRoot == null) {
            throw new IllegalArgumentException("El destino visual exige una carpeta de proyecto.");
        }
    }

    public Path outputDirectory() {
        Path base = projectRoot == null
                ? Path.of(".").toAbsolutePath().normalize()
                : projectRoot;
        Path resolved = base.resolve(relativeDirectory).toAbsolutePath().normalize();
        if (requireInsideProject && !resolved.startsWith(base)) {
            throw new IllegalStateException("El destino visual sale de la carpeta del proyecto.");
        }
        return resolved;
    }

    private static String safe(String value) {
        String normalized = value == null ? "" : value.strip().toLowerCase(java.util.Locale.ROOT);
        return normalized.replaceAll("[^a-z0-9._-]+", "-")
                .replaceAll("^-+|-+$", "")
                .replaceAll("-+", "-");
    }
}

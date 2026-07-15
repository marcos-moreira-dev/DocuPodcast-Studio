package com.marcosmoreiradev.docupodcaststudio.domain.process;

/** A project-relative artifact produced or consumed by a long-running process. */
public record ProcessJobArtifact(
        String role,
        String relativePath,
        String description
) {
    public ProcessJobArtifact {
        role = normalize(role).isBlank() ? "artifact" : normalize(role);
        relativePath = portableOptionalPath(relativePath);
        description = normalize(description);
    }

    public boolean present() {
        return !relativePath.isBlank();
    }

    private static String portableOptionalPath(String value) {
        String normalized = normalize(value).replace('\\', '/');
        if (normalized.isBlank()) {
            return "";
        }
        if (normalized.startsWith("/") || normalized.matches("^[A-Za-z]:/.*") || normalized.startsWith("../")
                || normalized.contains("/../") || normalized.startsWith("./") || normalized.contains("://")
                || normalized.toLowerCase().startsWith("file:")) {
            throw new IllegalArgumentException("Artifact path must be project-relative: " + value);
        }
        return normalized;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}

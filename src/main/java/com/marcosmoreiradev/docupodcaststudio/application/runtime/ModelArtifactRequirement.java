package com.marcosmoreiradev.docupodcaststudio.application.runtime;

import java.util.List;

/** One required or optional local artifact for an engine/runtime contract. */
public record ModelArtifactRequirement(
        String purpose,
        List<String> candidateRelativePaths,
        boolean required
) {
    public ModelArtifactRequirement {
        purpose = normalize(purpose).isBlank() ? "artefacto" : normalize(purpose);
        candidateRelativePaths = List.copyOf(candidateRelativePaths == null ? List.of() : candidateRelativePaths.stream()
                .map(ModelArtifactRequirement::normalize)
                .filter(value -> !value.isBlank())
                .toList());
        if (candidateRelativePaths.isEmpty()) {
            throw new IllegalArgumentException("artifact requirement needs at least one relative path or suffix");
        }
    }

    public static ModelArtifactRequirement required(String purpose, String relativePath) {
        return new ModelArtifactRequirement(purpose, List.of(relativePath), true);
    }

    public static ModelArtifactRequirement optional(String purpose, String relativePath) {
        return new ModelArtifactRequirement(purpose, List.of(relativePath), false);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip().replace('\\', '/');
    }
}

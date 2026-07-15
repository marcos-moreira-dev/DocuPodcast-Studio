package com.marcosmoreiradev.docupodcaststudio.application.resources;

import java.util.Objects;

/** Descriptor for one classpath resource exported for AI/human workflows. */
public record AiResourceDescriptor(
        String id,
        AiResourceKind kind,
        String classpathLocation,
        String targetRelativePath,
        boolean importable,
        String contract,
        String recommendedUse,
        String description
) {
    public AiResourceDescriptor {
        id = required(id, "id");
        Objects.requireNonNull(kind, "kind");
        classpathLocation = required(classpathLocation, "classpathLocation");
        targetRelativePath = required(targetRelativePath, "targetRelativePath");
        contract = contract == null ? "" : contract.trim();
        recommendedUse = recommendedUse == null ? "" : recommendedUse.trim();
        description = description == null ? "" : description.trim();
        if (targetRelativePath.startsWith("/") || targetRelativePath.contains("..")) {
            throw new IllegalArgumentException("targetRelativePath must be project-relative and safe: " + targetRelativePath);
        }
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}

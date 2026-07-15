package com.marcosmoreiradev.docupodcaststudio.application.examples;

/** Optional visual asset bundled with a demo example. */
public record ExampleAssetDescriptor(
        String displayName,
        String resourcePath,
        String fileName,
        String suggestedUse
) {
    public ExampleAssetDescriptor {
        displayName = normalize(displayName, "Imagen demo");
        resourcePath = require(resourcePath, "resourcePath");
        fileName = require(fileName, "fileName");
        suggestedUse = normalize(suggestedUse, "Asset visual disponible para storyboard.");
    }

    private static String require(String value, String field) {
        String normalized = normalize(value, "");
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return normalized;
    }

    private static String normalize(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.strip();
    }
}

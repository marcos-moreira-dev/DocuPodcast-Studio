package com.marcosmoreiradev.docupodcaststudio.application.examples;

import java.util.List;

/** Product-facing demo descriptor shown in the Ejemplos chooser. */
public record ExampleProjectDescriptor(
        String id,
        String title,
        String subtitle,
        String description,
        String defaultProjectName,
        String sourceDocumentResource,
        String sourceDocumentFileName,
        List<String> capabilities,
        List<ExampleAssetDescriptor> assets,
        List<ExampleVisualBindingDescriptor> visualBindings,
        String theatreMarkdownResource
) {
    public ExampleProjectDescriptor(String id,
                                    String title,
                                    String subtitle,
                                    String description,
                                    String defaultProjectName,
                                    String sourceDocumentResource,
                                    String sourceDocumentFileName,
                                    List<String> capabilities,
                                    List<ExampleAssetDescriptor> assets) {
        this(id, title, subtitle, description, defaultProjectName, sourceDocumentResource,
                sourceDocumentFileName, capabilities, assets, List.of(), "");
    }

    public ExampleProjectDescriptor {
        id = require(id, "id");
        title = require(title, "title");
        subtitle = normalize(subtitle, "Proyecto demo DocuPodcast");
        description = normalize(description, "Ejemplo incluido con DocuPodcast Studio.");
        defaultProjectName = normalize(defaultProjectName, title);
        sourceDocumentResource = require(sourceDocumentResource, "sourceDocumentResource");
        sourceDocumentFileName = require(sourceDocumentFileName, "sourceDocumentFileName");
        capabilities = capabilities == null ? List.of() : List.copyOf(capabilities);
        assets = assets == null ? List.of() : List.copyOf(assets);
        visualBindings = visualBindings == null ? List.of() : List.copyOf(visualBindings);
        theatreMarkdownResource = theatreMarkdownResource == null ? "" : theatreMarkdownResource.strip();
    }

    public boolean hasAssets() {
        return !assets.isEmpty();
    }

    public boolean hasTheatreMarkdown() {
        return !theatreMarkdownResource.isBlank();
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

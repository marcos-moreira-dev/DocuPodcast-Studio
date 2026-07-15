package com.marcosmoreiradev.docupodcaststudio.application.examples;

import java.nio.file.Path;
import java.util.List;

/** Files copied from classpath resources to a project staging folder. */
public record ExampleProjectMaterialization(
        ExampleProjectDescriptor example,
        Path projectFile,
        Path sourceDocument,
        List<Path> visualAssets,
        Path theatreMarkdownFile
) {
    public ExampleProjectMaterialization {
        if (example == null) {
            throw new IllegalArgumentException("example is required");
        }
        if (projectFile == null) {
            throw new IllegalArgumentException("projectFile is required");
        }
        if (sourceDocument == null) {
            throw new IllegalArgumentException("sourceDocument is required");
        }
        visualAssets = visualAssets == null ? List.of() : List.copyOf(visualAssets);
        theatreMarkdownFile = theatreMarkdownFile == null || !theatreMarkdownFile.toFile().exists() ? null : theatreMarkdownFile.toAbsolutePath().normalize();
    }
}

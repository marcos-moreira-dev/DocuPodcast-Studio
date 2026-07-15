package com.marcosmoreiradev.docupodcaststudio.application.resources;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

/** Exports official prompts, grammars, examples and templates for AI workflows. */
public final class ExportAiResourcesUseCase {
    private final AiResourceExporter exporter;

    public ExportAiResourcesUseCase(AiResourceExporter exporter) {
        this.exporter = Objects.requireNonNull(exporter, "exporter");
    }

    public AiResourceExportResult export(Path targetDirectory) throws IOException {
        return exporter.export(targetDirectory);
    }
}

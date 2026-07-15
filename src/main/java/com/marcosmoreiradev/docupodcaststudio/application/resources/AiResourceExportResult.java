package com.marcosmoreiradev.docupodcaststudio.application.resources;

import java.nio.file.Path;

/** Summary of an AI resources export operation. */
public record AiResourceExportResult(Path targetDirectory, Path indexPath, int exportedFiles) {
    public AiResourceExportResult {
        if (targetDirectory == null || indexPath == null) {
            throw new IllegalArgumentException("targetDirectory and indexPath are required");
        }
        if (exportedFiles < 0) {
            throw new IllegalArgumentException("exportedFiles cannot be negative");
        }
    }
}

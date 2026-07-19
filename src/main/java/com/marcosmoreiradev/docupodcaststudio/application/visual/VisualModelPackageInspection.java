package com.marcosmoreiradev.docupodcaststudio.application.visual;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/** Read-only preflight result for one shared visual model package. */
public record VisualModelPackageInspection(
        VisualModelManifest manifest,
        List<String> missingComponents,
        Map<String, List<Path>> duplicateFilesByHash,
        long freeBytes,
        long requiredBytes,
        List<String> diagnostics
) {
    public VisualModelPackageInspection {
        missingComponents = missingComponents == null ? List.of() : List.copyOf(missingComponents);
        duplicateFilesByHash = duplicateFilesByHash == null ? Map.of() : Map.copyOf(duplicateFilesByHash);
        freeBytes = Math.max(0L, freeBytes);
        requiredBytes = Math.max(0L, requiredBytes);
        diagnostics = diagnostics == null ? List.of() : List.copyOf(diagnostics);
    }

    public boolean ready() {
        return manifest != null && manifest.complete() && missingComponents.isEmpty();
    }

    public boolean hasSpace() {
        return requiredBytes == 0L || freeBytes >= requiredBytes;
    }
}

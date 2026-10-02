package com.marcosmoreiradev.docupodcaststudio.presentation.export;

import com.marcosmoreiradev.docupodcaststudio.application.export.ExportableArtifactKind;
import com.marcosmoreiradev.docupodcaststudio.presentation.command.AppCommandId;

import java.util.Objects;

/**
 * Auditable route from a visible export target to its controller operation and published artifact.
 */
public record ExportExecutionRoute(
        ExportableArtifactKind target,
        AppCommandId command,
        ExportControllerOperation operation,
        String artifactExtension
) {
    public ExportExecutionRoute {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(command, "command");
        Objects.requireNonNull(operation, "operation");
        artifactExtension = artifactExtension == null ? "" : artifactExtension.strip().toLowerCase();
        if (artifactExtension.isBlank() || !artifactExtension.startsWith(".")) {
            throw new IllegalArgumentException("artifact extension must start with a dot");
        }
    }
}

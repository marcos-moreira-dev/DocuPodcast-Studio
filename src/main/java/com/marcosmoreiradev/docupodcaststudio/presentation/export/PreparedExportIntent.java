package com.marcosmoreiradev.docupodcaststudio.presentation.export;

import com.marcosmoreiradev.docupodcaststudio.presentation.command.AppCommandId;

import java.nio.file.Path;
import java.util.Objects;

/** Immutable boundary captured before documentary preparation starts. */
public record PreparedExportIntent(
        AppCommandId commandId,
        ExportExecutionMode executionMode,
        Path targetFile,
        String outputLabel
) {
    public PreparedExportIntent {
        commandId = Objects.requireNonNull(commandId, "commandId");
        executionMode = executionMode == null ? ExportExecutionMode.READY_ONLY : executionMode;
        targetFile = Objects.requireNonNull(targetFile, "targetFile").toAbsolutePath().normalize();
        outputLabel = outputLabel == null || outputLabel.isBlank()
                ? commandId.name() : outputLabel.strip();
    }
}

package com.marcosmoreiradev.docupodcaststudio.presentation.export;

import com.marcosmoreiradev.docupodcaststudio.presentation.command.AppCommandId;

import java.util.Objects;

/** One creative export option shown by the central export dialog. */
public record ExportTargetPresentation(
        AppCommandId commandId,
        String title,
        String format,
        String targetHint,
        String readinessLabel,
        String detail,
        boolean executable,
        boolean preparable,
        String relatedProcessSummary
) {
    public ExportTargetPresentation {
        commandId = Objects.requireNonNull(commandId, "commandId");
        title = normalize(title, "Exportacion");
        format = normalize(format, "Salida");
        targetHint = normalize(targetHint, "Elegir destino al exportar");
        readinessLabel = normalize(readinessLabel, "Requiere revision");
        detail = normalize(detail, "");
        relatedProcessSummary = normalize(relatedProcessSummary, "Sin procesos relacionados registrados.");
    }

    public ExportTargetPresentation(
            AppCommandId commandId,
            String title,
            String format,
            String targetHint,
            String readinessLabel,
            String detail,
            boolean executable
    ) {
        this(commandId, title, format, targetHint, readinessLabel, detail, executable, executable,
                "Sin procesos relacionados registrados.");
    }

    public ExportTargetPresentation(
            AppCommandId commandId,
            String title,
            String format,
            String targetHint,
            String readinessLabel,
            String detail,
            boolean executable,
            boolean preparable
    ) {
        this(commandId, title, format, targetHint, readinessLabel, detail, executable, preparable,
                "Sin procesos relacionados registrados.");
    }

    @Override
    public String toString() {
        return title + " - " + format;
    }

    private static String normalize(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.strip();
    }
}

package com.marcosmoreiradev.docupodcaststudio.application.export;

import java.io.IOException;
import java.util.Objects;

/** Exports a portable folder containing input, editable project, outputs and reports. */
public final class ExportProjectBundleUseCase {
    private final ProjectBundleExporter exporter;

    public ExportProjectBundleUseCase(ProjectBundleExporter exporter) {
        this.exporter = Objects.requireNonNull(exporter, "exporter");
    }

    public ProjectBundleExportResult export(ProjectBundleExportRequest request) throws IOException {
        return exporter.export(request);
    }
}

package com.marcosmoreiradev.docupodcaststudio.application.export;

import java.io.IOException;

/** Port for exporting a project bundle to the filesystem. */
public interface ProjectBundleExporter {
    ProjectBundleExportResult export(ProjectBundleExportRequest request) throws IOException;
}

package com.marcosmoreiradev.docupodcaststudio.application.resources;

import java.io.IOException;
import java.nio.file.Path;

/** Copies official AI resources to a user-selected folder. */
public interface AiResourceExporter {
    AiResourceExportResult export(Path targetDirectory) throws IOException;
}

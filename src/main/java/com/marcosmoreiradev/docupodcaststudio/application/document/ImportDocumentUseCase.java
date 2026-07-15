package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/** Imports a source document through the first importer that supports it. */
public final class ImportDocumentUseCase {
    private final List<DocumentImporter> importers;

    public ImportDocumentUseCase(List<DocumentImporter> importers) {
        this.importers = List.copyOf(Objects.requireNonNull(importers, "importers"));
        if (this.importers.isEmpty()) {
            throw new IllegalArgumentException("At least one document importer is required");
        }
    }

    public ReadableDocument importDocument(Path sourceFile) throws IOException {
        Objects.requireNonNull(sourceFile, "sourceFile");
        for (DocumentImporter importer : importers) {
            if (importer.supports(sourceFile)) {
                return importer.importDocument(sourceFile);
            }
        }
        throw new IOException("No hay importador disponible para: " + sourceFile.getFileName());
    }
}

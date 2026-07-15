package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.ReadingProfile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

/** Port that writes an imported document and its source into the project folder. */
public interface ImportedDocumentWorkspaceRepository {
    MaterializedImportedDocument materialize(ReadableDocument document, ReadingProfile activeReadingProfile, Path projectFile) throws IOException;

    Optional<ReadableDocument> load(Path projectFile) throws IOException;
}

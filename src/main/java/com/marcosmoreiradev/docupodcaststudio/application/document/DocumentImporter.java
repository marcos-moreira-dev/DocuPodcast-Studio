package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;

import java.io.IOException;
import java.nio.file.Path;

/** Port implemented by source-specific document importers. */
public interface DocumentImporter {
    boolean supports(Path sourceFile);

    ReadableDocument importDocument(Path sourceFile) throws IOException;
}

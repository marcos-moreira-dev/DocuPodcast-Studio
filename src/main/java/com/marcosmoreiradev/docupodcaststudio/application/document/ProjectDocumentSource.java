package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentFormat;

import java.nio.file.Path;

/** Session document source without projecting canonical PDFs into document blocks. */
public sealed interface ProjectDocumentSource permits BlockDocumentSource, PreparedPdfSource {
    String title();

    SourceDocumentFormat format();

    Path sourcePath();
}

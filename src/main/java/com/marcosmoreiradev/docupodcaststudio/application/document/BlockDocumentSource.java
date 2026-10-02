package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentFormat;

import java.nio.file.Path;
import java.util.Objects;

/** Block-backed source for DOCX, TXT and Markdown. */
public record BlockDocumentSource(ReadableDocument document) implements ProjectDocumentSource {
    public BlockDocumentSource {
        document = Objects.requireNonNull(document, "document");
        if (document.format() == SourceDocumentFormat.PDF) {
            throw new IllegalArgumentException("PDF must use PreparedPdfSource");
        }
    }

    @Override public String title() { return document.title(); }
    @Override public SourceDocumentFormat format() { return document.format(); }
    @Override public Path sourcePath() { return document.sourcePath(); }
}

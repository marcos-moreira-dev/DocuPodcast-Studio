package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;

import java.util.List;

/** Optional provider for source-specific outline hints. Implementations must fail closed. */
@FunctionalInterface
public interface DocumentOutlineHintProvider {
    DocumentOutlineHintProvider NONE = document -> List.of();

    List<DocumentOutlineHint> hintsFor(ReadableDocument document);
}

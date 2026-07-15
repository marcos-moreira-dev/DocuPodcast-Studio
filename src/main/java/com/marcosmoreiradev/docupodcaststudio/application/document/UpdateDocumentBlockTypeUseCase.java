package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlockType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;

import java.util.Objects;

/** Applies a manual block classification override in the imported document review stage. */
public final class UpdateDocumentBlockTypeUseCase {
    public ReadableDocument update(ReadableDocument document, String blockId, DocumentBlockType type) {
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(type, "type");
        return document.withBlockType(blockId, type, "manual-document-workspace-action");
    }
}

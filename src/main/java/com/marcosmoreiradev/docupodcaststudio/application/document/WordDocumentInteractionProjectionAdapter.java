package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentProcessingScope;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;

/** Word selection adapter. ReadableDocument/DocumentBlock remain the canonical source. */
public final class WordDocumentInteractionProjectionAdapter
        implements DocumentInteractionProjectionAdapter<ReadableDocument, String> {
    @Override
    public DocumentInteractionProjection project(
            ReadableDocument source,
            String selectedBlockId,
            String preferredNarrationSegmentId,
            DocumentProcessingScope requestedScope,
            boolean narrationAvailable,
            boolean audioCoverageAvailable) {
        boolean selected = source != null && selectedBlockId != null
                && !selectedBlockId.isBlank();
        return new DocumentInteractionProjection(
                source == null ? DocumentInteractionProjection.SourceKind.NONE
                        : DocumentInteractionProjection.SourceKind.WORD,
                selected,
                selected ? preferredNarrationSegmentId : "",
                requestedScope,
                narrationAvailable,
                audioCoverageAvailable);
    }
}

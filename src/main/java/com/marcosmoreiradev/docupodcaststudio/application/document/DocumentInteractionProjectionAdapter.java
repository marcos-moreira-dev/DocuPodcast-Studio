package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentProcessingScope;

/** Adapts a canonical source and its native selection without leaking either downstream. */
public interface DocumentInteractionProjectionAdapter<S, T> {
    DocumentInteractionProjection project(
            S source,
            T selection,
            String preferredNarrationSegmentId,
            DocumentProcessingScope requestedScope,
            boolean narrationAvailable,
            boolean audioCoverageAvailable);
}

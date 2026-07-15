package com.marcosmoreiradev.docupodcaststudio.application.reading;

import com.marcosmoreiradev.docupodcaststudio.application.script.BuildNarrationScriptUseCase;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.ReadingProfile;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;

import java.util.Objects;

/**
 * Builds the prepared-reading projection used by the document-first product flow.
 *
 * <p>This use case is the public application boundary for preparing a document for audio. It
 * delegates to the legacy segment-based builder while that internal model is still required by
 * TTS, playback, render and project round-trip compatibility.</p>
 */
public final class BuildPreparedReadingProjectionUseCase {
    private final BuildNarrationScriptUseCase compatibilityBuilder;

    public BuildPreparedReadingProjectionUseCase(BuildNarrationScriptUseCase compatibilityBuilder) {
        this.compatibilityBuilder = Objects.requireNonNull(compatibilityBuilder, "compatibilityBuilder");
    }

    public PreparedReadingProjection build(ReadableDocument document, String language) {
        return build(document, language, false);
    }

    public PreparedReadingProjection build(ReadableDocument document, String language, boolean readAfterColon) {
        Objects.requireNonNull(document, "document");
        NarrationScriptDocument internalProjection = compatibilityBuilder.build(document, language, readAfterColon);
        return PreparedReadingProjection.from(document, internalProjection);
    }

    public PreparedReadingProjection build(ReadableDocument document, String language, boolean readAfterColon, ReadingProfile profile) {
        Objects.requireNonNull(document, "document");
        NarrationScriptDocument internalProjection = compatibilityBuilder.build(document, language, readAfterColon, profile);
        return PreparedReadingProjection.from(document, internalProjection);
    }
}

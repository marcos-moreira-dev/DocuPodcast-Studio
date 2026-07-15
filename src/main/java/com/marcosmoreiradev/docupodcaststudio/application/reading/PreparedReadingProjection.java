package com.marcosmoreiradev.docupodcaststudio.application.reading;

import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;

import java.time.Instant;
import java.util.Objects;

/**
 * User-facing prepared-reading projection built from a source document.
 *
 * <p>The historical {@link NarrationScriptDocument} is retained as the internal compatibility
 * payload for audio, render, playback and persisted projects. Product surfaces must talk about
 * documents, prepared readings, fragments and audio; they must not expose the legacy script model
 * as a separate user category.</p>
 */
public record PreparedReadingProjection(
        ReadableDocument sourceDocument,
        NarrationScriptDocument narrationScript,
        String label,
        Instant preparedAt
) {
    public PreparedReadingProjection {
        sourceDocument = Objects.requireNonNull(sourceDocument, "sourceDocument");
        narrationScript = Objects.requireNonNull(narrationScript, "narrationScript");
        label = label == null || label.isBlank() ? "Lectura preparada" : label.strip();
        preparedAt = preparedAt == null ? Instant.now() : preparedAt;
    }

    public static PreparedReadingProjection from(ReadableDocument sourceDocument, NarrationScriptDocument narrationScript) {
        return new PreparedReadingProjection(sourceDocument, narrationScript, "Lectura preparada", Instant.now());
    }

    public int fragmentCount() {
        return narrationScript.segmentCount();
    }

    public long narratableFragmentCount() {
        return narrationScript.narratableSegmentCount();
    }

    public long wordCount() {
        return narrationScript.wordCount();
    }

    public boolean empty() {
        return narrationScript.empty();
    }
}

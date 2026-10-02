package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentProcessingScope;

import java.util.Objects;

/** Complete audio execution intent restored after preparation or render resets. */
public record DocumentPlaybackIntent(
        DocumentAudioAction action,
        DocumentProcessingScope scope,
        boolean autoPlay,
        String preferredSegmentId,
        boolean singleCue
) {
    public DocumentPlaybackIntent {
        action = Objects.requireNonNullElse(action, DocumentAudioAction.GENERATE_ALL);
        scope = Objects.requireNonNullElse(scope, DocumentProcessingScope.FULL_DOCUMENT);
        preferredSegmentId = preferredSegmentId == null ? "" : preferredSegmentId.strip();
        singleCue = autoPlay && singleCue;
    }

    /** Compatibility constructor for existing playback-only callers. */
    public DocumentPlaybackIntent(boolean autoPlay, String preferredSegmentId,
                                  boolean singleCue) {
        this(autoPlay ? DocumentAudioAction.FAST_LISTEN
                        : DocumentAudioAction.GENERATE_ALL,
                preferredSegmentId == null || preferredSegmentId.isBlank()
                        ? DocumentProcessingScope.FULL_DOCUMENT
                        : DocumentProcessingScope.FROM_SELECTION,
                autoPlay, preferredSegmentId, singleCue);
    }

    public static DocumentPlaybackIntent none() {
        return forAction(DocumentAudioAction.GENERATE_ALL,
                DocumentProcessingScope.FULL_DOCUMENT, "", false, false);
    }

    public static DocumentPlaybackIntent fromBeginning() {
        return forAction(DocumentAudioAction.FAST_LISTEN,
                DocumentProcessingScope.FULL_DOCUMENT, "", true, false);
    }

    public static DocumentPlaybackIntent fromSegment(String segmentId, boolean singleCue) {
        return forAction(DocumentAudioAction.FAST_LISTEN,
                singleCue ? DocumentProcessingScope.SINGLE_FRAGMENT
                        : DocumentProcessingScope.FROM_SELECTION,
                segmentId, true, singleCue);
    }

    public static DocumentPlaybackIntent forAction(
            DocumentAudioAction action,
            DocumentProcessingScope scope,
            String preferredSegmentId,
            boolean autoPlay,
            boolean singleCue) {
        return new DocumentPlaybackIntent(action, scope, autoPlay,
                preferredSegmentId, singleCue);
    }
}

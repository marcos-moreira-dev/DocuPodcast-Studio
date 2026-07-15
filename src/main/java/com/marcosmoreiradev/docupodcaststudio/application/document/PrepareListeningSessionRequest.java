package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioJobStatusDto;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackBufferPolicy;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackManifest;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;

/** Immutable input for the product-level "Escuchar documento" orchestration use case. */
public record PrepareListeningSessionRequest(
        ReadableDocument document,
        NarrationScriptDocument narrationProjection,
        PlaybackManifest playbackManifest,
        boolean audioJobRunning,
        boolean projectSaved,
        AudioJobStatusDto audioJobStatus,
        PlaybackBufferPolicy bufferPolicy
) {
}

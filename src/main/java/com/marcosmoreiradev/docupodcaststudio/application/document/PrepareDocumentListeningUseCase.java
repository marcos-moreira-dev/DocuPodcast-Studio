package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioJobStatusDto;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackBufferPolicy;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackManifest;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;

/**
 * Decides the next step for the end-to-end "Escuchar documento" action.
 *
 * <p>This is deliberately a product/application use case. It prevents the main reader from becoming
 * a cabina de avión while keeping the real brain explicit: create the internal narration projection
 * when needed, reuse existing audio when possible, start from buffer when it is ready, or ask the
 * user to save before generating assets.</p>
 */
public final class PrepareDocumentListeningUseCase {
    public DocumentListenPlan prepare(ReadableDocument document,
                                      NarrationScriptDocument narrationProjection,
                                      PlaybackManifest playbackManifest,
                                      boolean audioJobRunning,
                                      boolean projectSaved,
                                      AudioJobStatusDto audioJobStatus,
                                      PlaybackBufferPolicy bufferPolicy) {
        if (document == null) {
            return DocumentListenPlan.noDocument();
        }
        if (document.narratableBlockCount() == 0) {
            return DocumentListenPlan.noNarratableText();
        }
        if (narrationProjection == null || narrationProjection.empty()) {
            return DocumentListenPlan.buildNarrationProjection();
        }
        if (playbackManifest != null && !playbackManifest.emptyManifest()) {
            return DocumentListenPlan.playExistingAudio();
        }
        if (audioJobRunning) {
            return DocumentListenPlan.waitForAudioBuffer(bufferLabel(audioJobStatus, bufferPolicy));
        }
        if (!projectSaved) {
            return DocumentListenPlan.saveProjectRequired();
        }
        return DocumentListenPlan.generateAudio();
    }

    private static String bufferLabel(AudioJobStatusDto status, PlaybackBufferPolicy policy) {
        if (status == null || policy == null) {
            return "";
        }
        int completed = status.completedSegments();
        int total = status.totalSegments();
        return "Fragmentos listos: %d/%d · inicia con %d.".formatted(completed, total, policy.initialReadySegments());
    }
}

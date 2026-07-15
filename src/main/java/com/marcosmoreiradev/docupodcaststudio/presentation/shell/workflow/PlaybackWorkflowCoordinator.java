package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioJobStatusDto;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackBufferPolicy;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackCue;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackCursor;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackManifest;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;

import java.util.Objects;
import java.util.Optional;

/**
 * Coordinates document-root playback decisions without owning JavaFX controls.
 *
 * <p>The shell still drives the visible transport, but this coordinator keeps the brain rules for
 * buffered playback, preferred document anchor and gap recovery outside the view-model.</p>
 */
public final class PlaybackWorkflowCoordinator {
    public boolean canStartBufferedPlayback(boolean documentPlaybackRequested,
                                            PlaybackCursor cursor,
                                            AudioJobStatusDto status,
                                            PlaybackManifest manifest,
                                            PlaybackBufferPolicy policy) {
        if (!documentPlaybackRequested || manifest == null || manifest.emptyManifest()) {
            return false;
        }
        if (cursor == null || !cursor.stoppedState()) {
            return false;
        }
        if (status == null || policy == null) {
            return false;
        }
        return policy.canStart(status.completedSegments(), status.totalSegments());
    }

    public Optional<PlaybackCue> preferredStartCue(PlaybackManifest manifest, Optional<NarrationSegment> preferredSegment) {
        if (manifest == null || manifest.emptyManifest()) {
            return Optional.empty();
        }
        Optional<NarrationSegment> normalized = preferredSegment == null ? Optional.empty() : preferredSegment;
        if (normalized.isPresent()) {
            // When the user explicitly selected a block/title/segment, do not fall back to the
            // first available cue. Falling back can skip the selected heading while nearby chunks
            // are still being rendered. The caller will wait until the requested cue exists.
            return manifest.cueForSegment(normalized.get().id());
        }
        return manifest.firstCue();
    }

    public boolean canContinueAfterGap(String waitingForBufferedSegmentAfter, PlaybackManifest manifest) {
        return waitingForBufferedSegmentAfter != null
                && !waitingForBufferedSegmentAfter.isBlank()
                && manifest != null
                && !manifest.emptyManifest();
    }

    public Optional<PlaybackCue> nextCueAfterGap(String waitingForBufferedSegmentAfter, PlaybackManifest manifest) {
        if (!canContinueAfterGap(waitingForBufferedSegmentAfter, manifest)) {
            return Optional.empty();
        }
        return manifest.nextCueAfterUnit(waitingForBufferedSegmentAfter);
    }

    public String waitingForBufferMessage(PlaybackBufferPolicy policy, String bufferStatusLabel) {
        Objects.requireNonNull(policy, "policy");
        String suffix = bufferStatusLabel == null ? "" : bufferStatusLabel.strip();
        if (suffix.isBlank()) {
            return policy.waitingLabel();
        }
        return policy.waitingLabel() + " " + suffix;
    }
}

package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackCue;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackCursor;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackManifest;

import java.util.Optional;

/**
 * Resolves the acoustic unit that owns the document's visual follow-along.
 *
 * <p>The transport cursor identifies a narration segment and an absolute time,
 * but one segment can contain several independently played sentence WAVs. The
 * concrete cue handed to the player is therefore more precise than resolving
 * the cursor again from the manifest.</p>
 */
final class PlaybackVisualCueResolver {
    private PlaybackVisualCueResolver() {
    }

    static Optional<PlaybackCue> resolve(
            PlaybackCursor cursor,
            PlaybackCue activeAcousticCue,
            PlaybackManifest manifest) {
        if (cursor == null || cursor.stoppedState()) {
            return Optional.empty();
        }
        if (activeAcousticCue != null
                && activeAcousticCue.segmentId().equals(cursor.segmentId())) {
            return Optional.of(activeAcousticCue);
        }
        if (manifest == null || manifest.emptyManifest()) {
            return Optional.empty();
        }
        Optional<PlaybackCue> byPosition = manifest.cueAt(cursor.positionSeconds())
                .filter(cue -> cue.segmentId().equals(cursor.segmentId()));
        return byPosition.isPresent() ? byPosition : manifest.cueForSegment(cursor.segmentId());
    }
}

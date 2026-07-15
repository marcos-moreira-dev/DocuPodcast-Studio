package com.marcosmoreiradev.docupodcaststudio.presentation.playback;

import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackCue;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackCursor;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackManifest;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardDocument;

import java.util.Objects;
import java.util.Optional;

/**
 * UI-agnostic synchronization projection for one narration segment.
 *
 * <p>Script, Audio and Storyboard all need the same answer: what segment is active, whether it
 * has a playback cue, whether the cue has audio, and whether storyboard imagery exists. Keeping
 * this projection outside JavaFX prevents each workspace from inventing a slightly different
 * interpretation of playback state.</p>
 */
public record PlaybackSegmentSync(
        String segmentId,
        String title,
        String narrationPreview,
        boolean cueAvailable,
        boolean audioReady,
        boolean storyboardReady,
        double startSeconds,
        double endSeconds,
        double currentPositionSeconds,
        boolean current,
        boolean playing,
        boolean paused,
        String audioRelativePath,
        String imageAssetId
) {
    public PlaybackSegmentSync {
        segmentId = normalize(segmentId);
        title = normalize(title);
        narrationPreview = normalize(narrationPreview);
        startSeconds = Math.max(0.0, startSeconds);
        endSeconds = Math.max(startSeconds, endSeconds);
        currentPositionSeconds = Math.max(0.0, currentPositionSeconds);
        audioRelativePath = normalize(audioRelativePath);
        imageAssetId = normalize(imageAssetId);
    }

    public static PlaybackSegmentSync from(
            NarrationSegment segment,
            PlaybackManifest manifest,
            StoryboardDocument storyboard,
            PlaybackCursor cursor
    ) {
        Objects.requireNonNull(segment, "segment");
        Optional<PlaybackCue> cue = manifest == null || manifest.emptyManifest()
                ? Optional.empty()
                : manifest.cueForSegment(segment.id());
        boolean storyboardFromCue = cue.map(PlaybackCue::hasImage).orElse(false);
        boolean storyboardFromDocument = storyboard != null && storyboard.bindingForSegment(segment.id()).isPresent();
        boolean sameCursorSegment = cursor != null && segment.id().equals(cursor.segmentId());
        return new PlaybackSegmentSync(
                segment.id(),
                segment.title().isBlank() ? segment.id() : segment.title(),
                segment.preview(120),
                cue.isPresent(),
                cue.map(PlaybackCue::hasAudio).orElse(false),
                storyboardFromCue || storyboardFromDocument,
                cue.map(PlaybackCue::startSeconds).orElse(0.0),
                cue.map(PlaybackCue::endSeconds).orElse(0.0),
                sameCursorSegment && cursor != null ? cursor.positionSeconds() : 0.0,
                sameCursorSegment,
                sameCursorSegment && cursor != null && !cursor.paused(),
                sameCursorSegment && cursor != null && cursor.paused(),
                cue.map(PlaybackCue::audioRelativePath).orElse(""),
                cue.map(PlaybackCue::imageAssetId)
                        .filter(value -> !value.isBlank())
                        .orElseGet(() -> storyboard == null ? "" : storyboard.bindingForSegment(segment.id())
                                .map(binding -> binding.imageAssetId())
                                .orElse(""))
        );
    }

    public String cueLabel() {
        if (!cueAvailable) {
            return segmentId + " · sin cue de audio";
        }
        return segmentId + " · " + Math.round(startSeconds) + "s-" + Math.round(endSeconds) + "s"
                + " · audio " + (audioReady ? audioRelativePath : "pendiente")
                + " · visuales " + (storyboardReady ? imageAssetId : "pendiente");
    }

    public String stateLabel() {
        if (playing) {
            return "Reproduciendo @ " + Math.round(currentPositionSeconds) + " s";
        }
        if (paused) {
            return "Pausado @ " + Math.round(currentPositionSeconds) + " s";
        }
        if (!cueAvailable) {
            return "Sin audio generado";
        }
        return "Listo para reproducir";
    }

    public String cardCssClass() {
        if (playing) {
            return "playback-sync-playing";
        }
        if (paused) {
            return "playback-sync-paused";
        }
        if (current) {
            return "playback-sync-selected";
        }
        return cueAvailable ? "playback-sync-ready" : "playback-sync-pending";
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}

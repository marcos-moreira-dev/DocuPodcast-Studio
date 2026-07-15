package com.marcosmoreiradev.docupodcaststudio.application.services;

import com.marcosmoreiradev.docupodcaststudio.application.playback.AdvancePlaybackCursorUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.playback.BuildPlaybackManifestUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.playback.SeekPlaybackUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.playback.SegmentAudioPlayer;
import com.marcosmoreiradev.docupodcaststudio.application.playback.NoopSegmentAudioPlayer;

/** Playback cursor, manifest construction and synchronized playback services. */
public record PlaybackApplicationServices(
        SeekPlaybackUseCase seekPlayback,
        AdvancePlaybackCursorUseCase advancePlaybackCursor,
        BuildPlaybackManifestUseCase buildPlaybackManifest,
        SegmentAudioPlayer segmentAudioPlayer,
        SegmentAudioPlayer backgroundAudioPlayer,
        SegmentAudioPlayer previewAudioPlayer
) {
    public PlaybackApplicationServices(SeekPlaybackUseCase seekPlayback,
                                       AdvancePlaybackCursorUseCase advancePlaybackCursor,
                                       BuildPlaybackManifestUseCase buildPlaybackManifest,
                                       SegmentAudioPlayer segmentAudioPlayer) {
        this(seekPlayback, advancePlaybackCursor, buildPlaybackManifest, segmentAudioPlayer,
                new NoopSegmentAudioPlayer(), new NoopSegmentAudioPlayer());
    }

    public PlaybackApplicationServices(SeekPlaybackUseCase seekPlayback,
                                       AdvancePlaybackCursorUseCase advancePlaybackCursor,
                                       BuildPlaybackManifestUseCase buildPlaybackManifest,
                                       SegmentAudioPlayer segmentAudioPlayer,
                                       SegmentAudioPlayer backgroundAudioPlayer) {
        this(seekPlayback, advancePlaybackCursor, buildPlaybackManifest, segmentAudioPlayer,
                backgroundAudioPlayer, new NoopSegmentAudioPlayer());
    }
}

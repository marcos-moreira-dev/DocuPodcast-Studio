package com.marcosmoreiradev.docupodcaststudio.application.playback;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Consumer;

/** Port for playing a single generated segment audio file. Implementations may be real or no-op. */
public interface SegmentAudioPlayer {
    void play(Path audioFile, double startSeconds) throws IOException;

    void pause();

    void resume();

    void stop();

    void setPlaybackRate(double rate);

    default void setVolume(double volume) {
        // Optional for secondary/background players.
    }

    default void setStopAtSeconds(double sourcePositionSeconds) {
        // Optional source-time cutoff for preview/background playback.
    }

    default void setFadeEnvelope(double sourceStartSeconds, double sourceEndSeconds, double fadeDurationSeconds) {
        // Optional source-time fade envelope for background tracks.
    }

    default void setOnPlaybackFinished(Consumer<Path> callback) {
        // Optional callback for real players that can report a natural end-of-WAV event.
    }

    double playbackRate();

    boolean available();

    boolean playing();

    double currentPositionSeconds();

    String statusLabel();
}

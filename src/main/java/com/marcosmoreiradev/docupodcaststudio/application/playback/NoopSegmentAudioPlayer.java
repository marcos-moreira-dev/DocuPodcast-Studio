package com.marcosmoreiradev.docupodcaststudio.application.playback;

import java.nio.file.Path;

/** Safe fallback audio player used when real audio playback is unavailable. */
public final class NoopSegmentAudioPlayer implements SegmentAudioPlayer {
    private double position;
    private boolean playing;
    private double playbackRate = 1.0;

    @Override
    public void play(Path audioFile, double startSeconds) {
        position = Math.max(0.0, startSeconds);
        playing = true;
    }

    @Override
    public void pause() {
        playing = false;
    }

    @Override
    public void resume() {
        playing = true;
    }

    @Override
    public void stop() {
        playing = false;
        position = 0.0;
    }

    @Override
    public void setPlaybackRate(double rate) {
        playbackRate = rate >= 1.74 ? 1.75 : rate >= 1.49 ? 1.5 : 1.0;
    }

    @Override
    public double playbackRate() {
        return playbackRate;
    }

    @Override
    public boolean available() {
        return false;
    }

    @Override
    public boolean playing() {
        return playing;
    }

    @Override
    public double currentPositionSeconds() {
        return position;
    }

    @Override
    public String statusLabel() {
        return "Playback sin dispositivo de audio real; se usará cursor sincronizado a " + playbackRate + "x.";
    }
}

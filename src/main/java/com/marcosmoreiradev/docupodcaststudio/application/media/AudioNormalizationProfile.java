package com.marcosmoreiradev.docupodcaststudio.application.media;

/** Target audio shapes used when FFmpeg prepares user media for DocuPodcast workflows. */
public enum AudioNormalizationProfile {
    ASSIGNABLE_AUDIO(44_100, 2, "audio asignable");

    private final int sampleRate;
    private final int channels;
    private final String displayName;

    AudioNormalizationProfile(int sampleRate, int channels, String displayName) {
        this.sampleRate = sampleRate;
        this.channels = channels;
        this.displayName = displayName;
    }

    public int sampleRate() {
        return sampleRate;
    }

    public int channels() {
        return channels;
    }

    public String displayName() {
        return displayName;
    }
}

package com.marcosmoreiradev.docupodcaststudio.application.process;

/** Coarse generation task family used to choose retry attempts consistently. */
public enum GenerationTaskKind {
    AUDIO_SEGMENT,
    AUDIO_BATCH,
    IMAGE_CANDIDATE,
    IMAGE_FRAME,
    IMAGE_BATCH;

    public boolean audio() {
        return this == AUDIO_SEGMENT || this == AUDIO_BATCH;
    }

    public boolean image() {
        return !audio();
    }
}

package com.marcosmoreiradev.docupodcaststudio.media.api;

/**
 * Product-neutral priority assigned before a local workload enters the shared
 * compute queue. Lower ranks are served first.
 */
public enum ComputeJobPriority {
    PLAYBACK_CRITICAL(0),
    USER_AUDIO(1),
    INTERACTIVE_ANALYSIS(2),
    AUDIO_LOOKAHEAD(3),
    BACKGROUND(4);

    private final int rank;

    ComputeJobPriority(int rank) {
        this.rank = rank;
    }

    public int rank() {
        return rank;
    }

    public ComputeJobPriority agedOneLevel() {
        return switch (this) {
            case BACKGROUND -> AUDIO_LOOKAHEAD;
            case AUDIO_LOOKAHEAD -> INTERACTIVE_ANALYSIS;
            default -> this;
        };
    }
}

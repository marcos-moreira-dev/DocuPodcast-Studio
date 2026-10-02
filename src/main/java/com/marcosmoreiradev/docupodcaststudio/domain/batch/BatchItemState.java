package com.marcosmoreiradev.docupodcaststudio.domain.batch;

/** Persistent lifecycle of one document in a Documentos a video Express project. */
public enum BatchItemState {
    PENDING,
    RUNNING,
    PAUSE_REQUESTED,
    PAUSED,
    COMPLETED,
    FAILED,
    SKIPPED,
    CANCELLED
}

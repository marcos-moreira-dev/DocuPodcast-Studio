package com.marcosmoreiradev.docupodcaststudio.domain.batch;

/** Recoverable production checkpoints. */
public enum BatchItemStage {
    DISCOVERED,
    SOURCE_COPIED,
    PROJECT_CREATED,
    DOCUMENT_PREPARATION,
    AUDIO_GENERATION,
    AUDIO_VERIFICATION,
    AUDIO_EXPORT,
    AUDIO_OUTPUT_VERIFICATION,
    VISUAL_PLAN,
    VIDEO_RENDER,
    VIDEO_VERIFICATION,
    FINISHED
}

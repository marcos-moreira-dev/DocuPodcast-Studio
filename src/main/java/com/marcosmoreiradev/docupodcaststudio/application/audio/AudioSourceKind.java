package com.marcosmoreiradev.docupodcaststudio.application.audio;

/** Operational audio source resolved for one fragment. */
public enum AudioSourceKind {
    GENERATED_TTS,
    RECORDED_HUMAN,
    IMPORTED_AUDIO,
    AMBIENT,
    NONE
}

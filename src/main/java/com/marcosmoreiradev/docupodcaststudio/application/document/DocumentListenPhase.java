package com.marcosmoreiradev.docupodcaststudio.application.document;

/**
 * High-level decisions for the user-facing "Escuchar documento" flow.
 *
 * <p>The phases describe the product intent, not JavaFX controls: the document remains the root
 * object, while narration projection, audio jobs, manifests and buffer are internal steps.</p>
 */
public enum DocumentListenPhase {
    NO_DOCUMENT,
    NO_NARRATABLE_TEXT,
    BUILD_NARRATION_PROJECTION,
    PLAY_EXISTING_AUDIO,
    WAIT_FOR_AUDIO_BUFFER,
    SAVE_PROJECT_REQUIRED,
    GENERATE_AUDIO
}

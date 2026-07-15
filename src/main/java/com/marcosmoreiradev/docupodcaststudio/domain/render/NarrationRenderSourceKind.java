package com.marcosmoreiradev.docupodcaststudio.domain.render;

/** Source used by one narration render unit. */
public enum NarrationRenderSourceKind {
    /** Generate speech with the configured TTS engine. */
    TEXT_TO_SPEECH,
    /** Use a user-selected audio clip as the main audio for the unit. */
    AUDIO_CLIP;

    public boolean tts() {
        return this == TEXT_TO_SPEECH;
    }

    public boolean externalAudio() {
        return this == AUDIO_CLIP;
    }
}

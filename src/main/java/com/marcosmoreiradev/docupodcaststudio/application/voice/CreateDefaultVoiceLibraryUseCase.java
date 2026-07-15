package com.marcosmoreiradev.docupodcaststudio.application.voice;

import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceLibrary;

/** Creates the built-in voice library used by new projects. */
public final class CreateDefaultVoiceLibraryUseCase {
    public VoiceLibrary create() {
        return VoiceLibrary.defaults();
    }
}

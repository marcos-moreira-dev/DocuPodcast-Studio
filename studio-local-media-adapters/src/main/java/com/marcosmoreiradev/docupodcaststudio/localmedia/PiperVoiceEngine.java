package com.marcosmoreiradev.docupodcaststudio.localmedia;

import com.marcosmoreiradev.docupodcaststudio.media.api.*;

import java.util.Set;

public final class PiperVoiceEngine extends AbstractProcessVoiceEngine {
    public static final EngineId ID = new EngineId("piper");

    public PiperVoiceEngine(EngineConfiguration configuration) {
        super(ID, "Voz local simple", Set.of(), configuration);
    }
}

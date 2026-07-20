package com.marcosmoreiradev.docupodcaststudio.localmedia;

import com.marcosmoreiradev.docupodcaststudio.media.api.*;

import java.util.Set;

public final class XttsVoiceEngine extends AbstractProcessVoiceEngine {
    public static final EngineId ID = new EngineId("xtts");

    public XttsVoiceEngine(EngineConfiguration configuration) {
        super(ID, "Voz IA avanzada",
                Set.of(EngineFeature.REFERENCE_VOICE, EngineFeature.EXPRESSIVE_STYLE, EngineFeature.BATCH),
                configuration);
    }
}

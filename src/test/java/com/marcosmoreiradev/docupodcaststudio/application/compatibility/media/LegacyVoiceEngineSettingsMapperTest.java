package com.marcosmoreiradev.docupodcaststudio.application.compatibility.media;

import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;
import com.marcosmoreiradev.docupodcaststudio.application.settings.TtsEngineModes;
import com.marcosmoreiradev.docupodcaststudio.media.api.*;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class LegacyVoiceEngineSettingsMapperTest {
    @Test
    void qwenKeepsItsNeutralEngineIdWhileMirroringAdvancedV1Mode() {
        EngineDescriptor qwen = new EngineDescriptor(new EngineId("qwen3-tts-local"),
                CapabilityId.VOICE_SYNTHESIS, "Qwen3-TTS local · 1.7B Q8", "1.7B-Q8_0",
                "llama.cpp-local-process",
                Set.of(EngineFeature.REFERENCE_VOICE, EngineFeature.EXPRESSIVE_STYLE), false);

        OperationalSettings selected = LegacyVoiceEngineSettingsMapper.select(
                OperationalSettings.defaults(), qwen);

        assertEquals("qwen3-tts-local", selected.mediaEngines().voiceEngineId());
        assertEquals(TtsEngineModes.ADVANCED_AI, selected.tts().engineMode());
    }
}

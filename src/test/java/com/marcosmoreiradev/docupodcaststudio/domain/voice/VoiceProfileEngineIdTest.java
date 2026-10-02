package com.marcosmoreiradev.docupodcaststudio.domain.voice;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class VoiceProfileEngineIdTest {
    @Test
    void legacyEnumsExposeStableEngineIds() {
        VoiceProfile profile = profile(VoiceEngineType.PIPER, Map.of());
        assertEquals("piper", profile.engineId().value());
    }

    @Test
    void futureEngineIdsRemainRoundTrippableWithUnknownLegacyAlias() {
        VoiceProfile profile = profile(VoiceEngineType.UNKNOWN, Map.of("engineId", "future-neural-voice"));
        assertEquals("future-neural-voice", profile.engineId().value());
        assertEquals(VoiceEngineType.UNKNOWN, VoiceProfile.engineTypeFor(profile.engineId().value()));
    }

    private static VoiceProfile profile(VoiceEngineType type, Map<String, String> metadata) {
        return new VoiceProfile("voice", "Voice", VoiceProfileType.PREDEFINED, type, "es", "", "",
                VoiceQualityPreset.BALANCED, false, "", metadata);
    }
}

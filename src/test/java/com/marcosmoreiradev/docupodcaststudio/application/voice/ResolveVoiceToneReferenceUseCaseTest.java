package com.marcosmoreiradev.docupodcaststudio.application.voice;

import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceEngineType;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceFileOwnership;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceLibrary;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceProfile;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceProfileType;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceQualityPreset;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceReferenceSample;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceReferenceTone;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceSampleOrigin;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ResolveVoiceToneReferenceUseCaseTest {
    private static final String TEST_ADVANCED_VOICE_ID = "VOC-TEST-ADVANCED";

    @Test
    void resolvesExactToneWhenAvailable() {
        VoiceLibrary library = testLibrary()
                .withReferenceSample(sample("S-NEUTRAL", VoiceReferenceTone.NEUTRAL))
                .withReferenceSample(sample("S-HAPPY", VoiceReferenceTone.HAPPY));

        VoiceToneReferenceResolution result = new ResolveVoiceToneReferenceUseCase()
                .resolve(library, TEST_ADVANCED_VOICE_ID, VoiceReferenceTone.HAPPY);

        assertTrue(result.available());
        assertFalse(result.fallbackToNeutral());
        assertEquals(VoiceReferenceTone.HAPPY, result.resolvedTone());
        assertEquals("S-HAPPY", result.sampleAssetId());
    }

    @Test
    void fallsBackToNeutralWhenRequestedToneIsMissing() {
        VoiceLibrary library = testLibrary()
                .withReferenceSample(sample("S-NEUTRAL", VoiceReferenceTone.NEUTRAL));

        VoiceToneReferenceResolution result = new ResolveVoiceToneReferenceUseCase()
                .resolve(library, TEST_ADVANCED_VOICE_ID, VoiceReferenceTone.HEROIC);

        assertTrue(result.available());
        assertTrue(result.fallbackToNeutral());
        assertEquals(VoiceReferenceTone.NEUTRAL, result.resolvedTone());
        assertEquals("S-NEUTRAL", result.sampleAssetId());
    }

    private static VoiceReferenceSample sample(String id, VoiceReferenceTone tone) {
        return new VoiceReferenceSample(id, TEST_ADVANCED_VOICE_ID, tone,
                "voices/samples/" + id + ".wav", VoiceSampleOrigin.IMPORTED_FILE,
                VoiceFileOwnership.PROJECT_ASSET, 1200L, Instant.now(), "test");
    }

    private static VoiceLibrary testLibrary() {
        return VoiceLibrary.defaults().withVoice(new VoiceProfile(
                TEST_ADVANCED_VOICE_ID,
                "Voz avanzada de prueba",
                VoiceProfileType.OWN,
                VoiceEngineType.XTTS,
                "es",
                "S-NEUTRAL",
                "",
                VoiceQualityPreset.HUMAN_REFERENCE,
                true,
                "Voz de prueba registrada por el usuario.",
                Map.of("userManaged", "true")
        ));
    }
}

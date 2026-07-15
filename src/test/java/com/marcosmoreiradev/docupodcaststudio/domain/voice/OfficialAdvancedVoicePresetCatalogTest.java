package com.marcosmoreiradev.docupodcaststudio.domain.voice;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class OfficialAdvancedVoicePresetCatalogTest {
    @Test
    void exposesBundledAdvancedPresetsAndSamples() {
        assertEquals(27, OfficialAdvancedVoicePresetCatalog.profiles().size());
        assertEquals(27, OfficialAdvancedVoicePresetCatalog.sampleSets().size());
        assertEquals(28, VoiceLibrary.defaults().voices().size());
        assertTrue(VoiceLibrary.defaults().voiceById(OfficialAdvancedVoicePresetCatalog.PRIMARY_PRESET_ID).isPresent());
        assertTrue(VoiceLibrary.defaults().voiceById("VOC-OWN-PLACEHOLDER").isEmpty());

        for (String voiceId : newOfficialVoiceIds()) {
            assertTrue(VoiceLibrary.defaults().voiceById(voiceId).isPresent(), voiceId);
        }

        Map<String, VoiceProfile> profilesById = OfficialAdvancedVoicePresetCatalog.profiles().stream()
                .collect(Collectors.toMap(VoiceProfile::id, profile -> profile));
        for (VoiceReferenceSampleSet sampleSet : OfficialAdvancedVoicePresetCatalog.sampleSets()) {
            VoiceProfile profile = profilesById.get(sampleSet.voiceProfileId());
            assertTrue(OfficialAdvancedVoicePresetCatalog.isOfficialPreset(profile));
            assertTrue(sampleSet.hasNeutral());
            assertEquals(38, sampleSet.samples().size());
            for (VoiceReferenceSample sample : sampleSet.samples()) {
                assertTrue(Files.isRegularFile(Path.of(sample.fileUri())), sample.fileUri());
            }
        }
    }

    @Test
    void spatialMapAndNewDefaultSpeakerAreVersioned() {
        assertTrue(Files.isRegularFile(Path.of("samples/theatre/maps/mapa-espacial.png")));
        assertTrue(Files.isRegularFile(Path.of("models/tts/xtts/speakers/voz-por-defecto.wav")));
        assertFalse(Files.exists(Path.of("samples/voices/default/source/voz-por-defecto.mp4")));
    }

    static List<String> newOfficialVoiceIds() {
        return List.of(
                "VOC-PRESET-HOMBRE-40-FIRME-LATAM-PERSONAJE",
                "VOC-PRESET-HOMBRE-40-CONVERSACIONAL-LATAM-DIALOGO",
                "VOC-PRESET-MUJER-20-ENTUSIASTA-LATAM-PERSONAJE",
                "VOC-PRESET-MUJER-20-SUAVE-COLOMBIANA-NEUTRAL-DIALOGO",
                "VOC-PRESET-HOMBRE-35-INTENSO-EXPLOSIVO-PERSONAJE",
                "VOC-PRESET-HOMBRE-45-RESIGNADO-OSCURO-PERSONAJE",
                "VOC-PRESET-HOMBRE-45-MORALISTA-COMICO-PERSONAJE",
                "VOC-PRESET-HOMBRE-35-ASPIRACIONAL-EMOTIVO-PERSONAJE",
                "VOC-PRESET-HOMBRE-40-INDIGNADO-TESTIGO-PERSONAJE",
                "VOC-PRESET-MUJER-40-DRAMATICA-INDIGNADA-PERSONAJE",
                "VOC-PRESET-MUJER-60-SABIA-MEMORIA-LATAM-PERSONAJE",
                "VOC-PRESET-NINO-10-TERCO-CARICATURESCO-PERSONAJE"
        );
    }
}

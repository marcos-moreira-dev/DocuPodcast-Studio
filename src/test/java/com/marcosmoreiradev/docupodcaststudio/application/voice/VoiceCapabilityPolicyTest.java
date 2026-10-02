package com.marcosmoreiradev.docupodcaststudio.application.voice;

import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioEngineDescriptor;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.OfficialAdvancedVoicePresetCatalog;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceLibrary;
import com.marcosmoreiradev.docupodcaststudio.media.api.EngineFeature;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class VoiceCapabilityPolicyTest {
    private final VoiceCapabilityPolicy policy = new VoiceCapabilityPolicy();

    @Test
    void defaultLibraryDistinguishesTestModeReadyAndAdvancedNeutralReference() {
        VoiceLibraryCapabilityReport report = policy.evaluate(VoiceLibrary.defaults(), AudioEngineDescriptor.mock());

        VoiceProfileCapability narrator = report.voice("VOC-NARRATOR").orElseThrow();
        assertTrue(narrator.assignable());
        assertTrue(narrator.synthesizableNow());
        assertEquals("Modo de prueba listo", narrator.status());

        VoiceProfileCapability advancedDefault = report.voice(OfficialAdvancedVoicePresetCatalog.PRIMARY_PRESET_ID).orElseThrow();
        assertTrue(advancedDefault.assignable());
        assertFalse(advancedDefault.synthesizableNow());
        assertFalse(advancedDefault.requiresSample());
        assertTrue(advancedDefault.referenceReady());
        assertEquals("Requiere Voz IA avanzada", advancedDefault.status());
        assertEquals("Hombre adulto narrativo", advancedDefault.displayName());
    }

    @Test
    void xttsEngineUsesPredesignedAdvancedNeutralReference() {
        AudioEngineDescriptor process = AudioEngineDescriptor.process(
                "Voz IA avanzada", true, "xtts {textFile} {outputFile}",
                "Configurado", java.util.Set.of(
                        EngineFeature.REFERENCE_VOICE,
                        EngineFeature.EXPRESSIVE_STYLE));
        VoiceLibraryCapabilityReport report = policy.evaluate(VoiceLibrary.defaults(), process);

        VoiceProfileCapability advancedDefault = report.voice(OfficialAdvancedVoicePresetCatalog.PRIMARY_PRESET_ID).orElseThrow();
        assertTrue(advancedDefault.assignable());
        assertTrue(advancedDefault.synthesizableNow());
        assertFalse(advancedDefault.requiresSample());
        assertTrue(advancedDefault.referenceReady());
        assertEquals("Voz IA avanzada lista", advancedDefault.status());
        assertEquals("Hombre adulto narrativo", advancedDefault.displayName());
    }

    @Test
    void processEngineAllowsDefaultVoiceButKeepsExpressiveStylesAsIntentions() {
        AudioEngineDescriptor process = AudioEngineDescriptor.process("TTS local", true, "tts {textFile} {outputFile}", "Configurado");
        VoiceLibraryCapabilityReport report = policy.evaluate(VoiceLibrary.defaults(), process);

        VoiceProfileCapability narrator = report.voice("VOC-NARRATOR").orElseThrow();
        assertTrue(narrator.synthesizableNow());
        assertEquals("TTS por defecto", narrator.status());

        PerformanceStyleCapability serious = report.style("STY-SERIOUS").orElseThrow();
        assertTrue(serious.assignable());
        assertFalse(serious.honoredByCurrentEngine());
        assertTrue(serious.roadmapOnly());
    }

    @Test
    void unconfiguredProcessEngineDoesNotPromiseSynthesis() {
        AudioEngineDescriptor process = AudioEngineDescriptor.process("TTS local", false, "", "Falta comando");
        VoiceLibraryCapabilityReport report = policy.evaluate(VoiceLibrary.defaults(), process);

        VoiceProfileCapability narrator = report.voice("VOC-NARRATOR").orElseThrow();
        assertFalse(narrator.synthesizableNow());
        assertTrue(narrator.requiresEngineConfiguration());
        assertEquals("Motor no configurado", narrator.status());
    }
}

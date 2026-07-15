package com.marcosmoreiradev.docupodcaststudio.application.voice;

import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioEngineDescriptor;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.OfficialAdvancedVoicePresetCatalog;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceLibrary;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BuildVoiceAssignmentOptionsUseCaseTest {
    private final BuildVoiceAssignmentOptionsUseCase useCase = new BuildVoiceAssignmentOptionsUseCase();

    @Test
    void currentVoiceStaysVisibleEvenWhenBlockedByEngine() {
        String current = OfficialAdvancedVoicePresetCatalog.PRIMARY_PRESET_ID;

        VoiceAssignmentOption option = option(VoiceLibrary.defaults(), AudioEngineDescriptor.mock(), current, current);

        assertTrue(option.current());
        assertFalse(option.selectable());
        assertEquals(VoiceReferenceAvailability.ENGINE_UNAVAILABLE, option.availability());
    }

    @Test
    void reservedVoicesAreNotSelectableUnlessTheyAreCurrent() {
        VoiceLibrary library = VoiceLibrary.defaults();

        VoiceAssignmentOption reserved = option(library, AudioEngineDescriptor.mock(), "VOC-NARRATOR", "", "VOC-NARRATOR");
        VoiceAssignmentOption current = option(library, AudioEngineDescriptor.mock(), "VOC-NARRATOR", "VOC-NARRATOR");

        assertTrue(reserved.reservedByOtherTarget());
        assertFalse(reserved.selectable());
        assertFalse(current.reservedByOtherTarget());
        assertTrue(current.current());
        assertTrue(current.selectable());
    }

    @Test
    void piperDoesNotPromiseAdvancedHumanVoices() {
        String advancedVoiceId = OfficialAdvancedVoicePresetCatalog.PRIMARY_PRESET_ID;

        VoiceAssignmentOption option = option(library(), piper(), "", advancedVoiceId);

        assertFalse(option.selectable());
        assertTrue(option.status().contains("Voz local simple") || option.detail().contains("Voz IA avanzada"));
    }

    @Test
    void xttsEnablesCompatibleOfficialPresetWhenConfigured() {
        String advancedVoiceId = OfficialAdvancedVoicePresetCatalog.PRIMARY_PRESET_ID;

        VoiceAssignmentOption option = option(library(), xtts(), "", advancedVoiceId);

        assertTrue(option.selectable());
        assertEquals(VoiceReferenceAvailability.BUILT_IN, option.availability());
    }

    @Test
    void mockKeepsNarratorUsableForTestMode() {
        VoiceAssignmentOption option = option(library(), AudioEngineDescriptor.mock(), "", "VOC-NARRATOR");

        assertTrue(option.selectable());
        assertEquals(VoiceReferenceAvailability.BUILT_IN, option.availability());
    }

    private VoiceAssignmentOption option(
            VoiceLibrary library,
            AudioEngineDescriptor engine,
            String reservedVoiceId,
            String voiceId
    ) {
        return option(library, engine, reservedVoiceId, voiceId, voiceId);
    }

    private VoiceAssignmentOption option(
            VoiceLibrary library,
            AudioEngineDescriptor engine,
            String reservedVoiceId,
            String currentVoiceId,
            String targetVoiceId
    ) {
        return useCase.build(library, engine, List.of(reservedVoiceId), currentVoiceId).stream()
                .filter(option -> option.voiceId().equals(targetVoiceId))
                .findFirst()
                .orElseThrow();
    }

    private static VoiceLibrary library() {
        return VoiceLibrary.defaults();
    }

    private static AudioEngineDescriptor piper() {
        return AudioEngineDescriptor.process("Voz local simple", true, "piper", "Configurado para prueba.");
    }

    private static AudioEngineDescriptor xtts() {
        return AudioEngineDescriptor.process("Voz IA avanzada", true, "xtts", "Configurado para prueba.");
    }
}

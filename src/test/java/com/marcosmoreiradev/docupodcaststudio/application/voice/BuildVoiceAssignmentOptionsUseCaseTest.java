package com.marcosmoreiradev.docupodcaststudio.application.voice;

import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioEngineDescriptor;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.OfficialAdvancedVoicePresetCatalog;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceLibrary;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceProfile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BuildVoiceAssignmentOptionsUseCaseTest {
    private final BuildVoiceAssignmentOptionsUseCase useCase = new BuildVoiceAssignmentOptionsUseCase();

    @TempDir
    Path temp;

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
        assertTrue(option.status().contains("sin referencias"));
        assertTrue(option.detail().contains("Coqui XTTS") && option.detail().contains("Qwen3-TTS"));
    }

    @Test
    void xttsEnablesCompatibleOfficialPresetWhenConfigured() {
        String advancedVoiceId = OfficialAdvancedVoicePresetCatalog.PRIMARY_PRESET_ID;

        VoiceAssignmentOption option = option(library(), xtts(), "", advancedVoiceId);

        assertTrue(option.selectable());
        assertEquals(VoiceReferenceAvailability.BUILT_IN, option.availability());
    }

    @Test
    void qwenEnablesTheSameOfficialPresetWithoutChangingItsProviderIdentity() {
        String advancedVoiceId = OfficialAdvancedVoicePresetCatalog.PRIMARY_PRESET_ID;

        VoiceAssignmentOption option = option(library(), qwen(), "", advancedVoiceId);

        assertTrue(option.selectable());
        assertEquals(VoiceReferenceAvailability.BUILT_IN, option.availability());
    }

    @Test
    void readinessContextKeepsMissingOfficialPresetVisibleButDisabled() {
        String advancedVoiceId = OfficialAdvancedVoicePresetCatalog.PRIMARY_PRESET_ID;
        VoiceLibrary library = library();

        VoiceAssignmentOption option = useCase.build(library, xtts(), List.of(), "",
                        readiness(temp.resolve("installation"), temp.resolve("runtime"), temp.resolve("project")))
                .stream().filter(candidate -> candidate.voiceId().equals(advancedVoiceId))
                .findFirst().orElseThrow();

        assertFalse(option.selectable());
        assertEquals(VoiceReferenceAvailability.MISSING_SAMPLE, option.availability());
        assertTrue(option.detail().contains("Biblioteca de voces"));
    }

    @Test
    void readinessContextEnablesOfficialPresetOnlyWhenNeutralFileResolves() throws Exception {
        VoiceLibrary library = library();
        String advancedVoiceId = OfficialAdvancedVoicePresetCatalog.PRIMARY_PRESET_ID;
        Path installation = temp.resolve("installation");
        var neutral = library.referenceSampleSetByVoiceId(advancedVoiceId).orElseThrow()
                .neutralSample().orElseThrow();
        Path file = installation.resolve(neutral.fileUri());
        Files.createDirectories(file.getParent());
        Files.write(file, new byte[] {1, 2, 3});

        VoiceAssignmentOption option = useCase.build(library, xtts(), List.of(), "",
                        readiness(installation, temp.resolve("runtime"), temp.resolve("project")))
                .stream().filter(candidate -> candidate.voiceId().equals(advancedVoiceId))
                .findFirst().orElseThrow();

        assertTrue(option.selectable());
        assertEquals(VoiceReferenceAvailability.BUILT_IN, option.availability());
    }

    @Test
    void legacyAdvancedNarratorResolvesItsPackagedDefaultSpeaker() throws Exception {
        Path installation = temp.resolve("installation");
        Path speaker = installation.resolve("models/tts/xtts/speakers/voz-por-defecto.wav");
        Files.createDirectories(speaker.getParent());
        Files.write(speaker, new byte[] {4, 5, 6});

        VoiceLibrary legacyLibrary = library().withVoice(VoiceProfile.ownVoicePlaceholder());
        VoiceAssignmentOption option = useCase.build(legacyLibrary, xtts(), List.of(), "",
                        readiness(installation, temp.resolve("runtime"), temp.resolve("project")))
                .stream().filter(candidate -> candidate.voiceId().equals("VOC-OWN-PLACEHOLDER"))
                .findFirst().orElseThrow();

        assertTrue(option.selectable());
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
        return AudioEngineDescriptor.process("Voz local simple", true, "piper",
                "Configurado para prueba.", java.util.Set.of(
                        com.marcosmoreiradev.docupodcaststudio.media.api.EngineFeature.PACKAGED_VOICE));
    }

    private static AudioEngineDescriptor xtts() {
        return AudioEngineDescriptor.process("Voz IA avanzada", true, "xtts",
                "Configurado para prueba.", java.util.Set.of(
                        com.marcosmoreiradev.docupodcaststudio.media.api.EngineFeature.REFERENCE_VOICE,
                        com.marcosmoreiradev.docupodcaststudio.media.api.EngineFeature.EXPRESSIVE_STYLE));
    }

    private static AudioEngineDescriptor qwen() {
        return AudioEngineDescriptor.process("Qwen3-TTS local · 1.7B Q8", true,
                "qwen3-tts-local", "Configurado para prueba.", java.util.Set.of(
                        com.marcosmoreiradev.docupodcaststudio.media.api.EngineFeature.REFERENCE_VOICE,
                        com.marcosmoreiradev.docupodcaststudio.media.api.EngineFeature.EXPRESSIVE_STYLE));
    }

    private static VoiceAssignmentReadinessContext readiness(
            Path installation, Path runtime, Path project) {
        return new VoiceAssignmentReadinessContext(installation, runtime, project);
    }
}

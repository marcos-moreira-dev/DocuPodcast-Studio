package com.marcosmoreiradev.docupodcaststudio.application.audio;

import com.marcosmoreiradev.docupodcaststudio.application.fragment.FragmentWorkspaceProjection;
import com.marcosmoreiradev.docupodcaststudio.application.voice.VoiceReferenceAvailability;
import com.marcosmoreiradev.docupodcaststudio.domain.assignment.NarrativeLayerAssignment;
import com.marcosmoreiradev.docupodcaststudio.domain.assignment.NarrativeLayerKind;
import com.marcosmoreiradev.docupodcaststudio.domain.fragment.DocumentFragment;
import com.marcosmoreiradev.docupodcaststudio.domain.fragment.FragmentAssetBinding;
import com.marcosmoreiradev.docupodcaststudio.domain.fragment.FragmentAssetRole;
import com.marcosmoreiradev.docupodcaststudio.domain.fragment.FragmentAssetSource;
import com.marcosmoreiradev.docupodcaststudio.domain.fragment.FragmentId;
import com.marcosmoreiradev.docupodcaststudio.domain.fragment.FragmentStatus;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegmentType;
import com.marcosmoreiradev.docupodcaststudio.domain.script.ScriptTextRange;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceEngineType;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceLibrary;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceProfile;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceProfileType;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceQualityPreset;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BuildAudioVoiceProductionProjectionUseCaseTest {
    private final BuildAudioVoiceProductionProjectionUseCase useCase = new BuildAudioVoiceProductionProjectionUseCase();

    @Test
    void resolvesExplicitVoiceAndImportedAudioByFragment() {
        FragmentId fragmentId = FragmentId.fromBlockId("B001");
        FragmentWorkspaceProjection source = projection(fragmentId,
                binding("AUD-1", fragmentId, "AUDIO-1", FragmentAssetRole.AUDIO_IMPORTED, "media/audio/clip.wav"),
                binding("VOICE-1", fragmentId, "VOC-CUSTOM", FragmentAssetRole.VOICE_TRACK, ""));
        VoiceLibrary library = VoiceLibrary.defaults().withVoice(voice("VOC-CUSTOM", VoiceEngineType.MOCK));

        AudioVoiceProductionProjection projection = useCase.build(source, script("VOC-NARRATOR"),
                DocuPodcastProject.createNew("Video"), library, List.of());
        FragmentAudioVoiceState state = projection.fragmentById(fragmentId).orElseThrow();

        assertEquals(AudioSourceKind.IMPORTED_AUDIO, state.audioSourceKind());
        assertTrue(state.audioReady());
        assertEquals("VOC-CUSTOM", state.voiceProfileId());
        assertEquals(VoiceReferenceAvailability.AVAILABLE, state.voiceAvailability());
        assertTrue(projection.readiness().readyForExport());
    }

    @Test
    void missingVoiceProfileIsReportedWithoutMutatingProjectLayer() {
        FragmentId fragmentId = FragmentId.fromBlockId("B001");
        NarrativeLayerAssignment missingVoice = new NarrativeLayerAssignment(
                "LYR-VOICE",
                NarrativeLayerKind.VOICE,
                new ScriptTextRange("SEG-001", 0, 4),
                "VOC-MISSING",
                "Voz borrada",
                "");
        DocuPodcastProject project = DocuPodcastProject.createNew("Documento").withNarrativeLayerAssignment(missingVoice);
        FragmentWorkspaceProjection source = projection(fragmentId,
                binding("VOICE-1", fragmentId, "VOC-MISSING", FragmentAssetRole.VOICE_TRACK, ""));

        FragmentAudioVoiceState state = useCase.build(source, script("VOC-NARRATOR"), project,
                VoiceLibrary.defaults(), List.of()).fragmentById(fragmentId).orElseThrow();

        assertEquals(VoiceReferenceAvailability.MISSING_PROFILE, state.voiceAvailability());
        assertFalse(state.voiceReady());
        assertEquals(1, project.narrativeLayerAssignments().size());
        assertEquals("VOC-MISSING", project.narrativeLayerAssignments().getFirst().targetId());
    }

    @Test
    void theatreVoiceAliasOverridesSegmentDefaultVoice() {
        FragmentId fragmentId = FragmentId.fromBlockId("B001");
        VoiceLibrary library = VoiceLibrary.defaults().withVoice(voice("VOC-CAPTAIN", VoiceEngineType.MOCK));
        TheatreProjectLayer theatre = new TheatreProjectLayer(
                List.of(TheatreProjectLayer.Intervencion.ofSequence(1, "B001")),
                List.of(),
                List.of(new TheatreProjectLayer.VoiceRoleAlias("VOICE-ROLE-CAP", "Capitan", "VOC-CAPTAIN", "CHR-CAP", "")),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(new TheatreProjectLayer.TextActionPlacement("INTERVENCION-1", "SCN-1", "CHR-CAP", "", "", "", Map.of())),
                List.of(),
                List.of());
        DocuPodcastProject project = DocuPodcastProject.createNew("Teatro").withTheatre(theatre);

        FragmentAudioVoiceState state = useCase.build(projection(fragmentId), script("VOC-NARRATOR"),
                project, library, List.of()).fragmentById(fragmentId).orElseThrow();

        assertEquals("VOC-CAPTAIN", state.voiceProfileId());
    }

    @Test
    void exposesTheProjectDefaultVoiceForNarratorFragments() {
        FragmentId fragmentId = FragmentId.fromBlockId("B001");
        VoiceLibrary library = VoiceLibrary.defaults().withVoice(
                voice("VOC-MARIA", VoiceEngineType.MOCK));
        DocuPodcastProject project = DocuPodcastProject.createNew("Documento")
                .withDocumentDefaultVoiceProfileId("VOC-MARIA");

        FragmentAudioVoiceState state = useCase.build(
                projection(fragmentId), script("VOC-NARRATOR"), project,
                library, List.of()).fragmentById(fragmentId).orElseThrow();

        assertEquals("VOC-MARIA", state.voiceProfileId());
    }

    @Test
    void engineReadinessBlocksWhenNoDocumentEngineIsUsable() {
        FragmentId fragmentId = FragmentId.fromBlockId("B001");
        List<AudioEngineReadinessUiItem> readiness = List.of(new AudioEngineReadinessUiItem(
                "xtts", "Voz IA avanzada", "Requiere reparacion", false, true,
                "No hay prueba WAV valida.", "Preparar motor"));

        AudioVoiceProductionProjection projection = useCase.build(projection(fragmentId), script("VOC-NARRATOR"),
                DocuPodcastProject.createNew("Documento"), VoiceLibrary.defaults(), readiness);

        assertFalse(projection.readiness().engineReady());
        assertTrue(projection.readiness().blockers().stream().anyMatch(message -> message.contains("motor de audio")));
    }

    private static FragmentWorkspaceProjection projection(FragmentId fragmentId, FragmentAssetBinding... bindings) {
        DocumentFragment fragment = new DocumentFragment(fragmentId, 0, "Hola mundo", "Bloque 1",
                null, "B001", "SEG-001", "", FragmentStatus.NARRATABLE, Map.of());
        return new FragmentWorkspaceProjection(List.of(fragment), List.of(bindings));
    }

    private static FragmentAssetBinding binding(String id, FragmentId fragmentId, String assetId,
                                                FragmentAssetRole role, String path) {
        return new FragmentAssetBinding(id, fragmentId, assetId, role, FragmentAssetSource.NARRATIVE_LAYER,
                path, "READY", assetId.isBlank() ? id : assetId, Map.of("durationSeconds", "1.5"));
    }

    private static NarrationScriptDocument script(String voiceId) {
        return NarrationScriptDocument.create("Lectura", "es", "doc.txt", List.of(new NarrationSegment(
                "SEG-001",
                NarrationSegmentType.PARAGRAPH,
                "Bloque 1",
                "Hola mundo",
                List.of("B001"),
                "CHR-NARRATOR",
                voiceId,
                "STY-NEUTRAL",
                Map.of())));
    }

    private static VoiceProfile voice(String id, VoiceEngineType engineType) {
        return new VoiceProfile(id, id, VoiceProfileType.OWN, engineType, "es", "", "",
                VoiceQualityPreset.BALANCED, false, "", Map.of());
    }
}

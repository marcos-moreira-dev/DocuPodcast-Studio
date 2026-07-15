package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioEngineDescriptor;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceEngineType;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceLibrary;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceProfile;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceProfileType;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceQualityPreset;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.ProjectSession;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TheatreChoralVoiceWorkflowTest {
    @Test
    void listsAllCharactersKeepsNarratorIndividualAndDisablesMissingVoice() {
        VoiceLibrary voices = VoiceLibrary.defaults().withVoice(new VoiceProfile(
                "VOC-A", "Voz A", VoiceProfileType.PREDEFINED, VoiceEngineType.LOCAL_TTS_PROCESS,
                "es", "", "", VoiceQualityPreset.BALANCED, false, "", Map.of()));
        TheatreProjectLayer theatre = new TheatreProjectLayer(
                List.of(new TheatreProjectLayer.Intervencion("INTERVENCION-1", "B0001", 1)),
                List.of(
                        new TheatreProjectLayer.CharacterProfile("CHR-NARRADOR", "NARRADOR", List.of(), ""),
                        new TheatreProjectLayer.CharacterProfile("CHR-A", "CAPITAN", List.of(), ""),
                        new TheatreProjectLayer.CharacterProfile("CHR-SIN-VOZ", "EXTRA", List.of(), "")),
                List.of(
                        new TheatreProjectLayer.VoiceRoleAlias("ALIAS-N", "Narrador", "VOC-NARRATOR", "CHR-NARRADOR", ""),
                        new TheatreProjectLayer.VoiceRoleAlias("ALIAS-A", "Capitan", "VOC-A", "CHR-A", "")),
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
        DocuPodcastProject project = DocuPodcastProject.createNew("Obra").withVoiceLibrary(voices).withTheatre(theatre);
        ProjectSession session = ProjectSession.opened(project, Path.of("obra.docupodcast.json"));

        List<TheatreChoralVoiceWorkflow.Option> options = new TheatreChoralVoiceWorkflow().options(
                Optional.of(session), Optional.of("INTERVENCION-1"),
                AudioEngineDescriptor.process("Motor TTS local", true, "tts", "listo"));

        assertEquals(3, options.size());
        assertTrue(options.stream().filter(TheatreChoralVoiceWorkflow.Option::narrator)
                .allMatch(TheatreChoralVoiceWorkflow.Option::voiceReady));
        assertTrue(options.stream().anyMatch(option -> option.characterId().equals("CHR-A") && option.voiceReady()));
        assertFalse(options.stream().filter(option -> option.characterId().equals("CHR-SIN-VOZ"))
                .findFirst().orElseThrow().voiceReady());
    }
}

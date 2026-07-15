package com.marcosmoreiradev.docupodcaststudio.infrastructure.json;

import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.OfficialAdvancedVoicePresetCatalog;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceFileOwnership;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceEngineType;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceLibrary;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceProfile;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceProfileType;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceQualityPreset;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceReferenceSample;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceReferenceSampleSet;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceReferenceTone;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceSampleOrigin;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DocuPodcastProjectVoiceLibraryJsonTest {
    @Test
    void writesAndReadsVoiceLibrarySection() throws Exception {
        DocuPodcastProject project = DocuPodcastProject.createNew("Voces").withVoiceLibrary(VoiceLibrary.defaults());
        String json = new DocuPodcastProjectJsonWriter().write(project);

        assertTrue(json.contains("\"voiceLibrary\""));
        assertTrue(json.contains("VOC-NARRATOR"));

        DocuPodcastProject opened = new DocuPodcastProjectJsonReader().read(json);
        assertEquals(1 + OfficialAdvancedVoicePresetCatalog.profiles().size(), opened.voiceLibrary().voices().size());
        assertEquals(1, opened.voiceLibrary().characters().size());
        assertEquals(8, opened.voiceLibrary().styles().size());
        assertEquals(OfficialAdvancedVoicePresetCatalog.sampleSets().size(), opened.voiceLibrary().referenceSampleSets().size());
        assertTrue(opened.voiceLibrary().voiceById(OfficialAdvancedVoicePresetCatalog.PRIMARY_PRESET_ID).isPresent());
    }

    @Test
    void roundTripsReferenceSampleSetsByTone() throws Exception {
        String voiceId = "VOC-TEST-ADVANCED";
        VoiceLibrary library = VoiceLibrary.defaults().withVoice(testAdvancedVoice(voiceId)).withReferenceSampleSet(new VoiceReferenceSampleSet(
                voiceId,
                List.of(
                        sample(voiceId, "VOICE-SAMPLE-VOC-TEST-ADVANCED-NEUTRAL", VoiceReferenceTone.NEUTRAL),
                        sample(voiceId, "VOICE-SAMPLE-VOC-TEST-ADVANCED-HAPPY", VoiceReferenceTone.HAPPY)
                )
        ));
        DocuPodcastProject project = DocuPodcastProject.createNew("Voces").withVoiceLibrary(library);

        String json = new DocuPodcastProjectJsonWriter().write(project);

        assertTrue(json.contains("\"referenceSampleSets\""));
        assertTrue(json.contains("\"tone\": \"HAPPY\""));
        DocuPodcastProject opened = new DocuPodcastProjectJsonReader().read(json);
        VoiceReferenceSampleSet openedSet = opened.voiceLibrary()
                .referenceSampleSetByVoiceId(voiceId)
                .orElseThrow();
        assertEquals(2, openedSet.samples().size());
        assertTrue(openedSet.sampleFor(VoiceReferenceTone.NEUTRAL).isPresent());
        assertTrue(openedSet.sampleFor(VoiceReferenceTone.HAPPY).isPresent());
    }

    @Test
    void readsLegacyVoiceLibraryWithoutReferenceSampleSets() throws Exception {
        DocuPodcastProject project = DocuPodcastProject.createNew("Voces").withVoiceLibrary(VoiceLibrary.defaults());
        String json = new DocuPodcastProjectJsonWriter().write(project)
                .replace("    \"referenceSampleSets\": [],\n", "");

        DocuPodcastProject opened = new DocuPodcastProjectJsonReader().read(json);

        assertEquals(1 + OfficialAdvancedVoicePresetCatalog.profiles().size(), opened.voiceLibrary().voices().size());
        assertEquals(OfficialAdvancedVoicePresetCatalog.sampleSets().size(), opened.voiceLibrary().referenceSampleSets().size());
    }

    @Test
    void backfillsMissingOfficialPresetsWhenOpeningOlderProject() throws Exception {
        VoiceLibrary legacyLibrary = new VoiceLibrary(
                "VOICE-LIBRARY-LEGACY",
                List.of(VoiceProfile.predefinedNarrator()),
                VoiceLibrary.defaults().characters(),
                VoiceLibrary.defaults().styles(),
                List.of(),
                Instant.parse("2026-06-01T00:00:00Z"),
                "Legacy voice library before bundled advanced presets were expanded."
        );
        DocuPodcastProject project = DocuPodcastProject.createNew("Voces").withVoiceLibrary(legacyLibrary);

        DocuPodcastProject opened = new DocuPodcastProjectJsonReader().read(new DocuPodcastProjectJsonWriter().write(project));

        assertEquals(1 + OfficialAdvancedVoicePresetCatalog.profiles().size(), opened.voiceLibrary().voices().size());
        assertEquals(OfficialAdvancedVoicePresetCatalog.sampleSets().size(), opened.voiceLibrary().referenceSampleSets().size());
        for (String voiceId : OfficialAdvancedVoicePresetCatalog.voiceIds()) {
            assertTrue(opened.voiceLibrary().voiceById(voiceId).isPresent(), voiceId);
            assertTrue(opened.voiceLibrary().referenceSampleSetByVoiceId(voiceId).isPresent(), voiceId);
        }
    }

    private static VoiceProfile testAdvancedVoice(String voiceId) {
        return new VoiceProfile(
                voiceId,
                "Voz avanzada de prueba",
                VoiceProfileType.OWN,
                VoiceEngineType.XTTS,
                "es",
                "VOICE-SAMPLE-VOC-TEST-ADVANCED-NEUTRAL",
                "",
                VoiceQualityPreset.HUMAN_REFERENCE,
                true,
                "Voz de prueba registrada por el usuario.",
                Map.of("userManaged", "true")
        );
    }

    private static VoiceReferenceSample sample(String voiceId, String id, VoiceReferenceTone tone) {
        return new VoiceReferenceSample(
                id,
                voiceId,
                tone,
                "voices/samples/" + id.toLowerCase() + ".wav",
                VoiceSampleOrigin.IMPORTED_FILE,
                VoiceFileOwnership.PROJECT_ASSET,
                0,
                Instant.parse("2026-01-01T00:00:00Z"),
                "Voz propia registrada"
        );
    }
}

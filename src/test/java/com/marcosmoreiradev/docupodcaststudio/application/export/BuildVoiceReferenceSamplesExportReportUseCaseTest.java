package com.marcosmoreiradev.docupodcaststudio.application.export;

import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetKind;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceEngineType;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceFileOwnership;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceLibrary;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceProfile;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceProfileType;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceQualityPreset;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceReferenceSample;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceReferenceSampleSet;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceReferenceTone;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceSampleOrigin;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BuildVoiceReferenceSamplesExportReportUseCaseTest {
    @Test
    void reportAuditsVoiceSamplesByToneAndGeneratedTests() throws Exception {
        Path root = Files.createTempDirectory("docupodcast-voice-export-report");
        Path projectFile = root.resolve("demo.docupodcast.json");
        Files.writeString(projectFile, "{}");
        Files.createDirectories(root.resolve("voices/samples"));
        Files.writeString(root.resolve("voices/samples/neutral.wav"), "wav");
        Files.createDirectories(root.resolve("voices/generated-tests/VOC-PEPITA"));
        Files.writeString(root.resolve("voices/generated-tests/VOC-PEPITA/test.wav"), "test");
        Files.writeString(root.resolve("voices/generated-tests/VOC-PEPITA/manifest.json"), "{}");

        VoiceProfile voice = new VoiceProfile(
                "VOC-PEPITA",
                "Pepita",
                VoiceProfileType.AUTHORIZED,
                VoiceEngineType.HUMAN_AUDIO,
                "es",
                "VOICE-SAMPLE-VOC-PEPITA-NEUTRAL",
                "",
                VoiceQualityPreset.HUMAN_REFERENCE,
                false,
                "Consentimiento registrado",
                Map.of());
        VoiceReferenceSample neutral = new VoiceReferenceSample(
                "VOICE-SAMPLE-VOC-PEPITA-NEUTRAL",
                voice.id(),
                VoiceReferenceTone.NEUTRAL,
                "voices/samples/neutral.wav",
                VoiceSampleOrigin.IMPORTED_FILE,
                VoiceFileOwnership.PROJECT_ASSET,
                1200,
                Instant.parse("2026-01-01T00:00:00Z"),
                "");
        VoiceReferenceSample happy = new VoiceReferenceSample(
                "VOICE-SAMPLE-VOC-PEPITA-HAPPY",
                voice.id(),
                VoiceReferenceTone.HAPPY,
                "voices/samples/happy.wav",
                VoiceSampleOrigin.IMPORTED_FILE,
                VoiceFileOwnership.PROJECT_ASSET,
                900,
                Instant.parse("2026-01-01T00:00:00Z"),
                "");
        VoiceLibrary library = focusedVoiceLibrary(voice)
                .withReferenceSampleSet(new VoiceReferenceSampleSet(voice.id(), List.of(neutral, happy)));
        DocuPodcastProject project = DocuPodcastProject.createNew("Demo")
                .withVoiceLibrary(library)
                .withAsset(new ProjectAssetReference(neutral.id(), ProjectAssetKind.VOICE_SAMPLE, "Neutral", neutral.fileUri(), "audio/wav", "Muestra", "sha256:abc", ""));

        VoiceReferenceSamplesExportReport report = new BuildVoiceReferenceSamplesExportReportUseCase()
                .build(project, projectFile);

        assertEquals(2, report.sampleCount());
        assertEquals(1, report.registeredAssetCount());
        assertEquals(1, report.missingFileCount());
        assertEquals(2, report.generatedTestFileCount());
        assertTrue(report.markdown().contains("Muestras de voz por tono"));
        assertTrue(report.markdown().contains("Pepita"));
        assertTrue(report.markdown().contains("fallback neutral"));
        assertTrue(report.tsv().contains("VOC-PEPITA"));
        assertTrue(report.tsv().contains("HAPPY"));
    }

    private static VoiceLibrary focusedVoiceLibrary(VoiceProfile voice) {
        VoiceLibrary defaults = VoiceLibrary.defaults();
        return new VoiceLibrary(
                "VOICE-LIBRARY-TEST",
                List.of(VoiceProfile.predefinedNarrator(), voice),
                defaults.characters(),
                defaults.styles(),
                List.of(),
                Instant.parse("2026-01-01T00:00:00Z"),
                "Biblioteca enfocada para reporte de muestras");
    }
}

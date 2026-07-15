package com.marcosmoreiradev.docupodcaststudio.application.voice;

import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetKind;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceLibrary;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceProfile;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceReferenceTone;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ImportVoiceSampleUseCaseTest {
    @Test
    void importsSampleAndLinksOwnVoiceProfile() throws Exception {
        Path temp = Files.createTempDirectory("voice-sample-usecase");
        Path projectFile = temp.resolve("MiProyecto.docupodcast.json");
        Files.writeString(projectFile, "{}");
        Path sample = temp.resolve("speaker.wav");
        Files.write(sample, new byte[] { 'R', 'I', 'F', 'F' });
        VoiceSampleRepository repository = new TestVoiceSampleRepository(false);

        VoiceSampleImportResult result = new ImportVoiceSampleUseCase(repository).importSample(
                DocuPodcastProject.empty("Proyecto"),
                mutableLegacyVoiceLibrary(),
                projectFile,
                VoiceSampleImportRequest.forOwnVoice("VOC-OWN-PLACEHOLDER", sample, "Mi voz")
        );

        assertEquals(ProjectAssetKind.VOICE_SAMPLE, result.sampleAsset().kind());
        assertEquals("VOICE-SAMPLE-VOC-OWN-PLACEHOLDER-NEUTRAL", result.sampleAsset().id());
        assertEquals("VOICE-SAMPLE-VOC-OWN-PLACEHOLDER-NEUTRAL", result.updatedVoiceProfile().sampleAssetId());
        assertEquals(VoiceReferenceTone.NEUTRAL, result.referenceSample().tone());
        assertTrue(result.project().assets().byId("VOICE-SAMPLE-VOC-OWN-PLACEHOLDER-NEUTRAL").isPresent());
        assertTrue(result.voiceLibrary().voiceById("VOC-OWN-PLACEHOLDER").orElseThrow().hasSample());
        assertTrue(result.voiceLibrary().referenceSampleSetByVoiceId("VOC-OWN-PLACEHOLDER").orElseThrow()
                .sampleFor(VoiceReferenceTone.NEUTRAL).isPresent());
    }

    @Test
    void importsDifferentToneWithoutOverwritingNeutralCompatibilitySample() throws Exception {
        Path temp = Files.createTempDirectory("voice-sample-usecase-tone");
        Path projectFile = temp.resolve("MiProyecto.docupodcast.json");
        Files.writeString(projectFile, "{}");
        Path neutralSample = temp.resolve("speaker-neutral.wav");
        Path happySample = temp.resolve("speaker-happy.wav");
        Files.write(neutralSample, new byte[] { 'R', 'I', 'F', 'F' });
        Files.write(happySample, new byte[] { 'R', 'I', 'F', 'F', 'H' });
        VoiceSampleRepository repository = new TestVoiceSampleRepository(false);
        ImportVoiceSampleUseCase useCase = new ImportVoiceSampleUseCase(repository);

        VoiceSampleImportResult neutral = useCase.importSample(
                DocuPodcastProject.empty("Proyecto"),
                mutableLegacyVoiceLibrary(),
                projectFile,
                VoiceSampleImportRequest.forOwnVoiceTone("VOC-OWN-PLACEHOLDER", neutralSample, "Mi voz neutral", VoiceReferenceTone.NEUTRAL)
        );
        VoiceSampleImportResult happy = useCase.importSample(
                neutral.project(),
                neutral.voiceLibrary(),
                projectFile,
                VoiceSampleImportRequest.forOwnVoiceTone("VOC-OWN-PLACEHOLDER", happySample, "Mi voz feliz", VoiceReferenceTone.HAPPY)
        );

        assertEquals("VOICE-SAMPLE-VOC-OWN-PLACEHOLDER-HAPPY", happy.sampleAsset().id());
        assertEquals("VOICE-SAMPLE-VOC-OWN-PLACEHOLDER-NEUTRAL", happy.updatedVoiceProfile().sampleAssetId());
        assertTrue(happy.voiceLibrary().referenceSampleSetByVoiceId("VOC-OWN-PLACEHOLDER").orElseThrow()
                .sampleFor(VoiceReferenceTone.NEUTRAL).isPresent());
        assertTrue(happy.voiceLibrary().referenceSampleSetByVoiceId("VOC-OWN-PLACEHOLDER").orElseThrow()
                .sampleFor(VoiceReferenceTone.HAPPY).isPresent());
    }


    @Test
    void applicationManagedSamplesStayOutsideProjectAssets() throws Exception {
        Path temp = Files.createTempDirectory("voice-sample-usecase-app");
        Path projectFile = temp.resolve("MiProyecto.docupodcast.json");
        Files.writeString(projectFile, "{}");
        Path sample = temp.resolve("speaker.wav");
        Files.write(sample, new byte[] { 'R', 'I', 'F', 'F' });
        VoiceSampleRepository repository = new TestVoiceSampleRepository(false) {
            @Override
            public boolean storesSamplesOutsideProject() {
                return true;
            }

            @Override
            public String referenceUriForImportedSample(Path projectFile, ProjectAssetReference sampleAsset) {
                return temp.resolve(sampleAsset.relativePath()).toAbsolutePath().normalize().toString();
            }
        };

        VoiceSampleImportResult result = new ImportVoiceSampleUseCase(repository).importSample(
                DocuPodcastProject.empty("Proyecto"),
                mutableLegacyVoiceLibrary(),
                projectFile,
                VoiceSampleImportRequest.forOwnVoice("VOC-OWN-PLACEHOLDER", sample, "Mi voz")
        );

        assertTrue(result.project().assets().byId("VOICE-SAMPLE-VOC-OWN-PLACEHOLDER-NEUTRAL").isEmpty());
        String storedUri = result.referenceSample().fileUri().replace('\\', '/');
        assertTrue(storedUri.endsWith("voices/samples/speaker.wav")
                || storedUri.contains("voice-library/samples/"),
                "La muestra gestionada por la app debe quedar fuera de los assets del proyecto y resolverse con separadores portables.");
        assertEquals(com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceFileOwnership.USER_APPDATA, result.referenceSample().ownership());
    }

    @Test
    void rejectsUnsupportedAudioExtension() throws Exception {
        Path temp = Files.createTempDirectory("voice-sample-usecase-bad");
        Path projectFile = temp.resolve("MiProyecto.docupodcast.json");
        Files.writeString(projectFile, "{}");
        Path sample = temp.resolve("speaker.txt");
        Files.writeString(sample, "not audio");
        VoiceSampleRepository repository = new TestVoiceSampleRepository(true);

        org.junit.jupiter.api.Assertions.assertThrows(IOException.class, () -> new ImportVoiceSampleUseCase(repository).importSample(
                DocuPodcastProject.empty("Proyecto"), mutableLegacyVoiceLibrary(), projectFile,
                new VoiceSampleImportRequest("VOC-OWN-PLACEHOLDER", sample, "Mi voz", com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceProfileType.OWN, "")
        ));
    }
    private static class TestVoiceSampleRepository implements VoiceSampleRepository {
        private final boolean failOnImport;

        private TestVoiceSampleRepository(boolean failOnImport) {
            this.failOnImport = failOnImport;
        }

        @Override
        public ProjectAssetReference importSample(Path projectFile, Path sourceAudioFile, String assetId, String displayName, String purpose, String notes) throws IOException {
            if (failOnImport) {
                throw new AssertionError("should not copy");
            }
            return new ProjectAssetReference(
                    assetId, ProjectAssetKind.VOICE_SAMPLE, displayName, "voices/samples/" + sourceAudioFile.getFileName(),
                    "audio/wav", purpose, "sha256:test", notes);
        }

        @Override
        public Path resolveManagedSample(Path projectFile, ProjectAssetReference sampleAsset) {
            return projectFile.getParent().resolve(sampleAsset.relativePath());
        }

        @Override
        public Path downloadSample(Path projectFile, ProjectAssetReference sampleAsset, Path targetDirectory, String preferredFileName) {
            return targetDirectory.resolve(preferredFileName == null || preferredFileName.isBlank() ? sampleAsset.displayName() : preferredFileName);
        }

        @Override
        public boolean deleteManagedSample(Path projectFile, ProjectAssetReference sampleAsset) {
            return true;
        }
    }

    private static VoiceLibrary mutableLegacyVoiceLibrary() {
        return VoiceLibrary.defaults().withVoice(VoiceProfile.ownVoicePlaceholder());
    }

}

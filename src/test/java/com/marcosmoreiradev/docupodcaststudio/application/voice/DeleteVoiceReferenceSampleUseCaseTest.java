package com.marcosmoreiradev.docupodcaststudio.application.voice;

import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetKind;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceFileOwnership;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceReferenceSample;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceReferenceTone;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceSampleOrigin;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.voice.LocalVoiceSampleFileRepository;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DeleteVoiceReferenceSampleUseCaseTest {
    @Test
    void deletesOnlyManagedProjectSamples() throws Exception {
        Path temp = Files.createTempDirectory("voice-delete-use-case");
        Path projectFile = temp.resolve("Proyecto.docupodcast.json");
        Files.writeString(projectFile, "{}");
        Path source = temp.resolve("maria-feliz.wav");
        Files.write(source, new byte[] { 'R', 'I', 'F', 'F', 5, 6 });
        LocalVoiceSampleFileRepository repository = new LocalVoiceSampleFileRepository();
        var asset = repository.importSample(projectFile, source, "VOICE-SAMPLE-MARIA-HAPPY", "María feliz", "Muestra feliz", "Voz autorizada");
        Path managed = repository.resolveManagedSample(projectFile, asset);
        VoiceReferenceSample sample = new VoiceReferenceSample(
                "SAMPLE-MARIA-HAPPY",
                "VOICE-MARIA",
                VoiceReferenceTone.HAPPY,
                asset.relativePath(),
                VoiceSampleOrigin.IMPORTED_FILE,
                VoiceFileOwnership.PROJECT_ASSET,
                0,
                Instant.now(),
                ""
        );

        var result = new DeleteVoiceReferenceSampleUseCase(repository).delete(projectFile, sample, asset);

        assertTrue(result.deleted());
        assertFalse(Files.exists(managed));
        assertTrue(Files.exists(source), "El archivo externo original no debe borrarse");
    }

    @Test
    void skipsExternalReferenceSamples() throws Exception {
        Path temp = Files.createTempDirectory("voice-delete-external");
        Path projectFile = temp.resolve("Proyecto.docupodcast.json");
        Files.writeString(projectFile, "{}");
        ProjectAssetReference asset = new ProjectAssetReference(
                "VOICE-SAMPLE-EXTERNAL",
                ProjectAssetKind.VOICE_SAMPLE,
                "Externa",
                "voices/samples/external.wav",
                "audio/wav",
                "Muestra externa",
                "",
                ""
        );
        VoiceReferenceSample sample = new VoiceReferenceSample(
                "SAMPLE-EXTERNAL",
                "VOICE-EXTERNAL",
                VoiceReferenceTone.NEUTRAL,
                "C:/Users/usuario/Downloads/external.wav",
                VoiceSampleOrigin.IMPORTED_FILE,
                VoiceFileOwnership.EXTERNAL_REFERENCE,
                0,
                Instant.now(),
                ""
        );

        var result = new DeleteVoiceReferenceSampleUseCase(new LocalVoiceSampleFileRepository()).delete(projectFile, sample, asset);

        assertFalse(result.deleted());
        assertTrue(result.message().contains("no borrará el archivo externo"));
    }
}

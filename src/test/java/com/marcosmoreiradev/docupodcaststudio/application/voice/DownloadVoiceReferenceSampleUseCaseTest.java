package com.marcosmoreiradev.docupodcaststudio.application.voice;

import com.marcosmoreiradev.docupodcaststudio.infrastructure.voice.LocalVoiceSampleFileRepository;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DownloadVoiceReferenceSampleUseCaseTest {
    @Test
    void downloadsManagedSampleToUserSelectedDirectoryWithoutMovingOriginal() throws Exception {
        Path temp = Files.createTempDirectory("voice-download-use-case");
        Path projectFile = temp.resolve("Proyecto.docupodcast.json");
        Files.writeString(projectFile, "{}");
        byte[] audio = new byte[] { 'R', 'I', 'F', 'F', 1, 2, 3, 4 };
        Path source = temp.resolve("maria-neutral.wav");
        Files.write(source, audio);
        LocalVoiceSampleFileRepository repository = new LocalVoiceSampleFileRepository();
        var asset = repository.importSample(projectFile, source, "VOICE-SAMPLE-MARIA-NEUTRAL", "María neutral", "Muestra neutral", "Voz autorizada");
        Path downloadDir = Files.createDirectory(temp.resolve("descargas"));

        var result = new DownloadVoiceReferenceSampleUseCase(repository).download(
                new VoiceSampleDownloadRequest(projectFile, asset, downloadDir, "maria-recuperada.wav"));

        assertTrue(Files.exists(result.copiedFile()));
        assertEquals("maria-recuperada.wav", result.copiedFile().getFileName().toString());
        assertArrayEquals(audio, Files.readAllBytes(result.copiedFile()));
        assertTrue(Files.exists(repository.resolveManagedSample(projectFile, asset)), "La descarga no debe mover ni borrar la muestra gestionada");
    }
}

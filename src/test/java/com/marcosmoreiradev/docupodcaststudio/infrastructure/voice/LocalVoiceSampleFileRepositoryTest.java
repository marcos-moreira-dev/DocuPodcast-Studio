package com.marcosmoreiradev.docupodcaststudio.infrastructure.voice;

import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetKind;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class LocalVoiceSampleFileRepositoryTest {
    @Test
    void copiesSampleToVoicesSamplesAndReturnsRelativeAsset() throws Exception {
        Path temp = Files.createTempDirectory("voice-sample-repo");
        Path projectFile = temp.resolve("MiProyecto.docupodcast.json");
        Files.writeString(projectFile, "{}");
        Path source = temp.resolve("speaker.wav");
        Files.write(source, new byte[] { 'R', 'I', 'F', 'F', 0, 1, 2, 3 });

        var asset = new LocalVoiceSampleFileRepository().importSample(
                projectFile, source, "VOICE-SAMPLE-VOC-OWN", "Mi voz", "Muestra de voz", "Voz propia");

        assertEquals(ProjectAssetKind.VOICE_SAMPLE, asset.kind());
        assertTrue(asset.relativePath().startsWith("voices/samples/"));
        assertTrue(asset.relativePath().endsWith(".wav"));
        assertTrue(asset.checksum().startsWith("sha256:"));
        assertTrue(Files.exists(temp.resolve(asset.relativePath())));
    }
}

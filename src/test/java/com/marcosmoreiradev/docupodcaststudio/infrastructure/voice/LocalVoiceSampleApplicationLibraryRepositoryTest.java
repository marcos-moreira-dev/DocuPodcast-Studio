package com.marcosmoreiradev.docupodcaststudio.infrastructure.voice;

import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetKind;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class LocalVoiceSampleApplicationLibraryRepositoryTest {
    @Test
    void productWiringStoresSamplesInApplicationVoiceLibraryOutsideProjectFolder() throws Exception {
        Path temp = Files.createTempDirectory("voice-sample-app-library");
        Path appRoot = temp.resolve("app");
        Path projectRoot = temp.resolve("project");
        Files.createDirectories(appRoot);
        Files.createDirectories(projectRoot);
        Path projectFile = projectRoot.resolve("MiProyecto.docupodcast.json");
        Files.writeString(projectFile, "{}");
        Path source = temp.resolve("speaker.wav");
        Files.write(source, new byte[] { 'R', 'I', 'F', 'F', 0, 1, 2, 3 });

        LocalVoiceSampleFileRepository repository = new LocalVoiceSampleFileRepository(appRoot);
        var asset = repository.importSample(projectFile, source, "VOICE-SAMPLE-MARIA-NEUTRAL", "María neutral", "Muestra", "");

        assertEquals(ProjectAssetKind.VOICE_SAMPLE, asset.kind());
        assertTrue(repository.storesSamplesOutsideProject());
        assertTrue(asset.relativePath().startsWith("voice-library/samples/"));
        assertTrue(Files.exists(appRoot.resolve(asset.relativePath())));
        assertFalse(Files.exists(projectRoot.resolve(asset.relativePath())));
        assertEquals(appRoot.resolve(asset.relativePath()).normalize().toString(), repository.referenceUriForImportedSample(projectFile, asset));
    }
}

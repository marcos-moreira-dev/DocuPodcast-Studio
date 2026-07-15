package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class VoiceSampleStorageT121V03SourceTest {
    @Test
    void voiceSamplesCanBeDownloadedAndSafelyDeleted() throws Exception {
        String repository = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/voice/VoiceSampleRepository.java"));
        String download = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/voice/DownloadVoiceReferenceSampleUseCase.java"));
        String delete = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/voice/DeleteVoiceReferenceSampleUseCase.java"));

        assertTrue(repository.contains("downloadSample"));
        assertTrue(repository.contains("deleteManagedSample"));
        assertTrue(repository.contains("resolveManagedSample"));
        assertTrue(download.contains("targetDirectory"));
        assertTrue(download.contains("Muestra descargada correctamente"));
        assertTrue(delete.contains("sample.canDeleteManagedFile()"));
        assertTrue(delete.contains("no borrará el archivo externo"));
    }

    @Test
    void repositoryKeepsManagedSamplesInsideVoicesSamples() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/voice/LocalVoiceSampleFileRepository.java"));
        assertTrue(source.contains("LEGACY_SAMPLE_DIR = \"voices/samples\""));
        assertTrue(source.contains("APP_SAMPLE_DIR = \"voice-library/samples\""));
        assertTrue(source.contains("storesSamplesOutsideProject"));
        assertTrue(source.contains("resolved.startsWith(managedRoot)"));
        assertTrue(source.contains("uniqueTarget"));
        assertTrue(source.contains("StandardCopyOption.COPY_ATTRIBUTES"));
    }
}

package com.marcosmoreiradev.docupodcaststudio.presentation.settings;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class EngineDownloadUrlCentralizationHf9HSourceTest {
    @Test
    void downloadUrlsAreCentralizedInSettingsAndEnvironmentOverrides() throws Exception {
        String settings = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/settings/OperationalSettings.java"));
        String repository = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/settings/PropertiesOperationalSettingsRepository.java"));

        assertTrue(settings.contains("DEFAULT_XTTS_DOWNLOAD_BASE_URL"));
        assertTrue(settings.contains("DOCUPODCAST_XTTS_DOWNLOAD_BASE_URL"));
        assertTrue(settings.contains("DOCUPODCAST_PIPER_RUNTIME_ZIP_URL"));
        assertTrue(repository.contains("download.xtts.baseUrl"));
        assertTrue(repository.contains("download.piper.runtimeZipUrl"));
        assertTrue(repository.contains("download.piper.defaultVoiceUrl"));
    }

    @Test
    void settingsDialogExposesEditableDownloadUrlsForBothVoiceEngines() throws Exception {
        String dialog = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsDialog.java"));

        assertTrue(dialog.contains("Página oficial del modelo de voz"));
        assertTrue(dialog.contains("URL runtime local"));
        assertTrue(dialog.contains("URL voz neutral"));
        assertTrue(dialog.contains("downloadUrlControl"));
    }

    @Test
    void downloadUseCasesUseConfiguredUrlsInsteadOfHardcodedOnly() throws Exception {
        String xtts = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/modelsetup/DownloadXttsOfficialModelUseCase.java"));
        String piper = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/modelsetup/DownloadPiperPortableRuntimeUseCase.java"));

        assertTrue(xtts.contains("modelFileUri(settings"));
        assertTrue(xtts.contains("xttsDownloadBaseUrl"));
        assertTrue(piper.contains("runtimeZipUri(settings)"));
        assertTrue(piper.contains("defaultVoiceUri(settings)"));
        assertTrue(piper.contains("defaultVoiceMetadataUri(settings)"));
    }
}

package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** DOWNLOAD-HF5 keeps advanced voice download failures visible in the normal setup flow. */
final class AdvancedVoiceDownloadVisibilityHf5SourceTest {
    @Test
    void settingsKeepsDownloadReportWhenAutomaticSetupFails() throws Exception {
        String settings = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/AdvancedVoiceSettingsOperations.java"));
        String humanizer = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsTechnicalMessageHumanizer.java"));
        assertTrue(settings.contains("XttsModelDownloadReport finalDownloadReport"));
        assertTrue(settings.contains("describeAdvancedVoiceSetupFailure(finalDownloadReport, finalReadiness, finalPreparationReport)"));
        assertTrue(settings.contains("friendlyDownloadFailures"));
        assertTrue(humanizer.contains("text.contains(\"http\")"));
        assertTrue(humanizer.contains("downloadFailure"));
    }

    @Test
    void downloaderStoresHumanReadableRequiredFileFailures() throws Exception {
        String downloader = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/modelsetup/DownloadXttsOfficialModelUseCase.java"));
        assertTrue(downloader.contains("humanFileName(file.name()) + \" (HTTP \""));
        assertTrue(downloader.contains("readableException"));
        assertTrue(downloader.contains("No se pudo descargar Voz IA avanzada"));
    }
}

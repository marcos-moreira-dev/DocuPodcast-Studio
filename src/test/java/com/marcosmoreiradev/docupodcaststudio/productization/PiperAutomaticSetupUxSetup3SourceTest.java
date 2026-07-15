package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** UX-SETUP3 protects automatic setup for the lightweight local voice path. */
final class PiperAutomaticSetupUxSetup3SourceTest {
    @Test
    void settingsOffersPiperAutomaticPreparationWithoutTechnicalScaffolding() throws Exception {
        String settings = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsDialog.java"));
        String piperSettings = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/PiperSettingsOperations.java"));
        String initialSetup = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/InitialSetupSettingsOperations.java"));
        String settingsSurface = settings + piperSettings + initialSetup;
        assertTrue(settingsSurface.contains("Preparar Voz local simple"));
        assertTrue(settings.contains("confirmAndPreparePiper"));
        assertTrue(settingsSurface.contains("downloadPiperPortableRuntime"));
        assertTrue(settings.contains("selectAndSavePiper") || settingsSurface.contains("piperOperations.selectAndSave"));
    }

    @Test
    void piperDownloaderPreparesRuntimeAndDefaultVoice() throws Exception {
        String useCase = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/modelsetup/DownloadPiperPortableRuntimeUseCase.java"));
        String policy = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/runtime/PiperVoiceModelPathPolicy.java"));
        assertTrue(useCase.contains("piper_windows_amd64.zip"));
        assertTrue(useCase.contains("PiperVoiceModelPathPolicy.DEFAULT_PIPER_VOICE"));
        assertTrue(policy.contains("es_ES-default-medium.onnx"));
        assertTrue(useCase.contains("copyWithProgress"));
        assertTrue(useCase.contains("InspectPiperSetupReadinessUseCase"));
    }

    @Test
    void servicesWirePiperDownloaderForInAppSetup() throws Exception {
        String services = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/services/SettingsApplicationServices.java"));
        String factory = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/bootstrap/ApplicationServicesFactory.java"));
        assertTrue(services.contains("DownloadPiperPortableRuntimeUseCase downloadPiperPortableRuntime"));
        assertTrue(factory.contains("new DownloadPiperPortableRuntimeUseCase()"));
    }
}

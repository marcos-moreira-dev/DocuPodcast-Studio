package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** PF2B guardrail: advanced voice setup must be operated from Settings, not from a required manual script UX. */
final class AdvancedVoiceInAppSetupPf2BSourceTest {
    @Test
    void settingsOffersRealInAppPrepareImportVerifyAndSafeSelectActions() throws Exception {
        String settings = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsDialog.java"));
        String advancedVoice = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/AdvancedVoiceSettingsOperations.java"));
        String settingsSurface = settings + advancedVoice;
        String services = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/services/SettingsApplicationServices.java"));
        String factory = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/bootstrap/ApplicationServicesFactory.java"));

        assertTrue(settings.contains("Preparar"));
        assertTrue(settings.contains("confirmUser"));
        assertTrue(settingsSurface.contains("PrepareXttsPortableRuntimeUseCase"));
        assertTrue(settingsSurface.contains("DirectoryChooser"));
        assertTrue(settingsSurface.contains("DownloadXttsOfficialModelUseCase"));
        assertTrue(settings.contains("Descargar"));
        assertTrue(settingsSurface.contains("ImportXttsModelFolderUseCase"));
        assertTrue(settings.contains("selectXttsIfReady") || settings.contains("Usar"));
        assertTrue(settings.contains("Solo se prepara cuando lo pides aquí") || settings.contains("Preparar"));
        assertTrue(settingsSurface.contains("Voz IA avanzada aún requiere preparación")
                || settingsSurface.contains("aún requiere preparación"));
        assertTrue(services.contains("PrepareXttsPortableRuntimeUseCase prepareXttsPortableRuntime"));
        assertTrue(services.contains("DownloadXttsOfficialModelUseCase downloadXttsOfficialModel"));
        assertTrue(services.contains("ImportXttsModelFolderUseCase importXttsModelFolder"));
        assertTrue(factory.contains("new PrepareXttsPortableRuntimeUseCase"));
        assertTrue(factory.contains("new DownloadXttsOfficialModelUseCase()"));
        assertTrue(factory.contains("new ImportXttsModelFolderUseCase()"));
    }

    @Test
    void runtimePreparationIsAUseCaseAndModelContractRequiresExactLocalModelFile() throws Exception {
        String prepare = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/modelsetup/PrepareXttsPortableRuntimeUseCase.java"));
        String importer = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/modelsetup/ImportXttsModelFolderUseCase.java"));
        String contract = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/modelsetup/ModelFolderContract.java"));
        String downloader = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/modelsetup/DownloadXttsOfficialModelUseCase.java"));
        String module = Files.readString(Path.of("src/main/java/module-info.java"));
        String engineCard = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/EngineSetupCard.java"));

        assertTrue(prepare.contains("ExternalProcessRunner"));
        assertTrue(prepare.contains("ExternalProcessRequest"));
        assertTrue(prepare.contains("ExternalProcessObserver"));
        assertTrue(prepare.contains("does not run at startup"));
        assertTrue(prepare.contains("powershell.exe"));
        assertTrue(importer.contains("Files.walk(source)"));
        assertTrue(importer.contains("models/tts/xtts"));
        assertTrue(contract.contains("model.pth"));
        assertTrue(contract.contains("speakers_xtts.pth"));
        assertTrue(contract.contains("dvae.pth"));
        assertTrue(contract.contains("mel_stats.pth"));
        assertTrue(downloader.contains("https://huggingface.co/coqui/XTTS-v2"));
        assertTrue(downloader.contains("normalizeDownloadResolveBaseUrl"));
        assertTrue(downloader.contains("COQUI_TTS_SOURCE_REPOSITORY_URL"));
        assertTrue(downloader.contains("model.pth"));
        assertTrue(module.contains("requires java.net.http"));
        assertFalse(contract.contains(".safetensors"));
        assertTrue(engineCard.contains("Guía informativa, no botón"));
    }
}

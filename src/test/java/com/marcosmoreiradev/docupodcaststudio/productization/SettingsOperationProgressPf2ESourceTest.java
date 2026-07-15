package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** PF2E guardrail: long voice setup operations must show live in-app progress, not a frozen dialog or manual-script UX. */
final class SettingsOperationProgressPf2ESourceTest {
    @Test
    void settingsProgressDialogReceivesLiveUpdatesFromUseCases() throws Exception {
        String settings = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsDialog.java"));
        String advancedVoice = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/AdvancedVoiceSettingsOperations.java"));
        String piper = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/PiperSettingsOperations.java"));
        String videoLocal = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/VideoLocalSettingsOperations.java"));
        String initialSetup = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/InitialSetupSettingsOperations.java"));
        String settingsSurface = settings + advancedVoice + piper + videoLocal + initialSetup;
        String progress = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsOperationProgressCoordinator.java"));
        String prepare = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/modelsetup/PrepareXttsPortableRuntimeUseCase.java"));
        String download = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/modelsetup/DownloadXttsOfficialModelUseCase.java"));
        String importer = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/modelsetup/ImportXttsModelFolderUseCase.java"));
        String listener = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/modelsetup/ModelSetupProgressListener.java"));

        assertTrue(settingsSurface.contains("progress.update") || settingsSurface.contains("progress::update"));
        assertTrue(progress.contains("Estado en vivo"));
        assertTrue(progress.contains("Última actualización"));
        assertTrue(listener.contains("onProgress"));
        assertTrue(prepare.contains("ExternalProcessObserver"));
        assertTrue(prepare.contains("onOutputLine"));
        assertTrue(download.contains("copyWithProgress"));
        assertTrue(download.contains("Descargando "));
        assertTrue(importer.contains("Copiados "));
    }

    @Test
    void diagnosticsPointUsersBackToSettingsInsteadOfRequiredManualScripts() throws Exception {
        String ttsConfig = Files.readString(Path.of("scripts/04-verificar-tts-config.bat"));
        assertTrue(ttsConfig.contains("ruta normal es Configuracion"));
        assertTrue(ttsConfig.contains("Script tecnico opcional/rescate"));
        assertFalse(ttsConfig.contains("Ejecuta: scripts\\30-preparar-voz-ia-avanzada-local.bat"));
    }
}

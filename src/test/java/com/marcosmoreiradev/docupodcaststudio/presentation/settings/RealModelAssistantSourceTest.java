package com.marcosmoreiradev.docupodcaststudio.presentation.settings;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RealModelAssistantSourceTest {
    @Test
    void settingsExposeRealLocalModelVerificationWithoutPollutingReader() throws Exception {
        String moduleInfo = Files.readString(Path.of("src/main/java/module-info.java"));
        String settings = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsDialog.java"));
        String view = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/ModelInstallAssistantView.java"));
        String catalog = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/ModelInstallAssistantCatalog.java"));
        String contract = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/modelsetup/ModelFolderContract.java"));
        String useCase = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/modelsetup/InspectLocalModelFolderUseCase.java"));
        String css = Files.readString(Path.of("src/main/resources/css/components/settings-shell.css"));
        String document = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java"));

        assertTrue(moduleInfo.contains("application.modelsetup"));
        assertFalse(settings.contains("ModelInstallAssistantView"));
        assertFalse(view.contains("new Button"));
        assertFalse(view.contains("ui-model-action-button"));
        assertTrue(catalog.contains("Verificar carpeta local con InspectLocalModelFolderUseCase")
                || catalog.contains("Verificar la instalación local"));
        assertTrue(contract.contains("XTTS / Coqui"));
        assertTrue(contract.contains("Piper"));
        assertFalse(contract.contains("Whisper local"));
        assertFalse(settings.contains("Whisper"));
        assertTrue(useCase.contains("Files.walk"));
        assertTrue(useCase.contains("sha256"));
        assertFalse(css.contains("ui-model-action-button"));
        assertFalse(catalog.contains("https://"));
        assertFalse(catalog.contains("http://"));
        assertFalse(document.contains("InspectLocalModelFolderUseCase"));
        assertFalse(document.contains("checksum"));
    }
}

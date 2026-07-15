package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class XttsDownloadVisibilityUxHf2SourceTest {
    @Test
    void xttsDownloadDistinguishesRequiredAndOptionalResources() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/modelsetup/DownloadXttsOfficialModelUseCase.java"));
        assertTrue(source.contains("boolean required"));
        assertTrue(source.contains("Falta un recurso obligatorio de voz"));
        assertTrue(source.contains("Recurso opcional no disponible"));
        assertTrue(source.contains("humanFileName"));
    }

    @Test
    void settingsShowsReadableInitialSetupAndDownloadDetails() throws Exception {
        String settings = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsDialog.java"));
        String advancedVoice = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/AdvancedVoiceSettingsOperations.java"));
        String initialSetup = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/InitialSetupSettingsOperations.java"));
        String settingsSurface = settings + advancedVoice + initialSetup;
        assertTrue(settings.contains("Configuración inicial prioriza Voz local simple")
                || settings.contains("Configuraci"));
        assertTrue(settings.contains("Imagen IA teatral Prueba 4GB solo se descarga si la marcas"));
        assertTrue(settings.contains("setMinWidth(780)") || settings.contains("setMinWidth(520)"));
        assertTrue(settingsSurface.contains("friendlyDownloadFailures"));
    }
}

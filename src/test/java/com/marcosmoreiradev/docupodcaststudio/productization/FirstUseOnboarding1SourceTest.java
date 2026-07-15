package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** FIRST-USE-ONBOARDING1 keeps the first run focused on listening quickly. */
final class FirstUseOnboarding1SourceTest {
    @Test
    void initialSetupPrioritizesLocalSimpleVoiceAndOffersImage4gbExplicitly() throws Exception {
        String settings = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsDialog.java"));
        String initialSetup = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/InitialSetupSettingsOperations.java"));
        String settingsSurface = settings + initialSetup;

        assertTrue(settingsSurface.contains("Prepara primero la voz rapida"));
        assertTrue(settingsSurface.contains("Voz local simple para que escuches documentos cuanto antes"));
        assertTrue(settingsSurface.contains("DownloadPiperPortableRuntimeUseCase downloader"));
        assertTrue(settingsSurface.contains("piperOperations.selectAndSave(form, services)"));
        assertTrue(settingsSurface.contains("Voz IA avanzada puede tardar mucho mas"));
        assertTrue(settingsSurface.contains("Imagen IA teatral Prueba 4GB"));
        assertTrue(settingsSurface.contains("imageChoice.setSelected(false)"));
        assertTrue(settingsSurface.contains("Confirmar descarga Prueba 4GB"));
        assertTrue(settingsSurface.contains("downloadLocalTheatreImagePackage"));
        assertTrue(settingsSurface.contains("Configuracion inicial completada"));
    }

    @Test
    void welcomeAndGuideDescribeFastListeningAndMp4Final() throws Exception {
        String welcome = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/welcome/WelcomeWorkspaceView.java"));
        String gettingStarted = Files.readString(Path.of("src/main/resources/help/topics/getting-started.md"));
        String exporting = Files.readString(Path.of("src/main/resources/help/topics/exporting.md"));
        String troubleshooting = Files.readString(Path.of("src/main/resources/help/topics/troubleshooting.md"));

        assertTrue(welcome.contains("Voz local simple para escuchar"));
        assertTrue(welcome.contains("Voz IA avanzada opcional"));
        assertTrue(gettingStarted.contains("Configuraci"));
        assertTrue(gettingStarted.contains("escuchar r"));
        assertTrue(exporting.contains("Video MP4 final"));
        assertTrue(troubleshooting.contains("prepara Video local"));
        assertFalse(exporting.contains("paquete renderizable"));
        assertFalse(troubleshooting.contains("paquete renderizable"));
    }
}

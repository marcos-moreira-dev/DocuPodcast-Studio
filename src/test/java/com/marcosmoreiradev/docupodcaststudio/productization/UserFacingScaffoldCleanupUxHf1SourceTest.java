package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** UX-HF1 keeps the normal application free of technical scaffolding. */
final class UserFacingScaffoldCleanupUxHf1SourceTest {
    @Test
    void welcomeGuideAndInitialSetupAreRealActions() throws Exception {
        String welcome = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/welcome/WelcomeWorkspaceView.java"));
        String shell = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java"));
        assertTrue(welcome.contains("Configuración inicial"));
        assertTrue(welcome.contains("Guía rápida"));
        assertTrue(welcome.contains("openGuide"));
        assertTrue(shell.contains("handleOpenFirstUseSetup"));
        assertTrue(shell.contains("AppCommandId.OPEN_GUIDE"));
        assertTrue(welcome.contains("commandRow(AppCommandId.OPEN_GUIDE") && welcome.contains("openGuide)"));
    }

    @Test
    void normalSettingsDoesNotExposePackagingOrDiagnosticScaffolding() throws Exception {
        String settings = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsDialog.java"));
        assertFalse(settings.contains("Inventario local de motores"));
        assertFalse(settings.contains("Auditar artefactos locales"));
        assertFalse(settings.contains("Generar checklist smoke GUI"));
        assertFalse(settings.contains("ENGINE_ARTIFACTS_AUDIT.md"));
        assertFalse(settings.contains("target\\\\docupodcast-engine-setup"));
        assertFalse(settings.contains("models/tts/piper/voices"));
        assertFalse(settings.contains("tools/ffmpeg/bin"));
    }
}

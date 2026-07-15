package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** UX-SETUP2 adds a user-facing first-use setup flow on top of existing engine actions. */
final class FirstUseSetupUxSetup2SourceTest {
    @Test
    void settingsDialogCanOpenDirectlyOnVoiceEnginesAndPromptInitialSetup() throws Exception {
        String settings = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsDialog.java"));
        String advancedVoice = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/AdvancedVoiceSettingsOperations.java"));
        String initialSetup = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/InitialSetupSettingsOperations.java"));
        String settingsSurface = settings + advancedVoice + initialSetup;
        assertTrue(settings.contains("showVoiceEngines"));
        assertTrue(settings.contains("showFirstUseSetup"));
        assertTrue(settings.contains("promptInitialSetup"));
        assertTrue(settings.contains("runInitialSetup"));
        assertTrue(settingsSurface.contains("Preparando este equipo"));
        assertTrue(settingsSurface.contains("Configuracion inicial completada"));
    }

    @Test
    void welcomeUsesFirstUseSetupInsteadOfGenericSettingsForMainSetup() throws Exception {
        String welcome = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/welcome/WelcomeWorkspaceView.java"));
        String shell = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java"));
        assertTrue(welcome.contains("Escucha r"));
        assertTrue(shell.contains("this::handleOpenFirstUseSetup"));
        assertTrue(shell.contains("settingsDialog.showFirstUseSetup"));
        assertTrue(shell.contains("settingsDialog.showVoiceEngines"));
    }
}

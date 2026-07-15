package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** UX-HF3 protects the build fix for the initial setup dialog. */
final class BuildGreenSettingsDialogUxHf3SourceTest {
    @Test
    void initialSetupDialogUsesChecklistAndEscapedLineBreaks() throws Exception {
        String settings = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsDialog.java"));
        String initialSetup = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/InitialSetupSettingsOperations.java"));
        String settingsSurface = settings + initialSetup;
        assertTrue(settingsSurface.contains("new CheckBox"));
        assertTrue(settingsSurface.contains("\\n\\n"));
        assertFalse(settingsSurface.contains("DocuPodcast puede preparar los recursos necesarios para empezar.\n\n\""));
    }

    @Test
    void shellRemainsBelowRf2Limit() throws Exception {
        long lines = Files.lines(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java")).count();
        assertTrue(lines <= 2700, "DocuPodcastShellViewModel debe mantenerse bajo el limite transitorio RF-TX2: " + lines);
    }
}

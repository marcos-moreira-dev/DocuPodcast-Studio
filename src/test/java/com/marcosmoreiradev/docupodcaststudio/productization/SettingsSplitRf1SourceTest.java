package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class SettingsSplitRf1SourceTest {
    @Test
    void settingsDialogDelegatesBottomActionBarToOperationalComponent() throws Exception {
        String dialog = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsDialog.java"));
        String actionBar = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsActionBar.java"));
        String docs = Files.readString(Path.of("docs/productizacion/SETTINGS_SPLIT_GUI_COMPONENTS_RF1.md"));

        assertTrue(dialog.contains("SettingsActionBar.create("));
        assertTrue(actionBar.contains("Restaurar predeterminados"));
        assertTrue(actionBar.contains("Guardar cambios"));
        assertTrue(actionBar.contains("OperationalStatusStrip"));
        assertTrue(docs.contains("SETTINGS-SPLIT-RF1"));
        assertTrue(docs.contains("barra inferior operativa"));
    }
}

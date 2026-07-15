package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class GuiComponentsRf1SourceTest {
    @Test
    void operationalStatusStripIsReusableAndNotDecorativeDashboard() throws Exception {
        String component = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/OperationalStatusStrip.java"));
        String settingsActionBar = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsActionBar.java"));
        String css = Files.readString(Path.of("src/main/resources/css/settings.css"));

        assertTrue(component.contains("final class OperationalStatusStrip"));
        assertTrue(component.contains("communicates the current state or next step"));
        assertFalse(component.toLowerCase().contains("dashboard card"));
        assertTrue(settingsActionBar.contains("new OperationalStatusStrip"));
        assertTrue(css.contains(".ui-operational-status-strip"));
        assertTrue(css.contains(".ui-operational-status-message"));
    }
}

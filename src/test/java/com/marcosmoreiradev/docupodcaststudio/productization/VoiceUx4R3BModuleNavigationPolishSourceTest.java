package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** VOZ-UX4R-3B: module navigation must be sober, clickable and not reset to Manage on refresh. */
final class VoiceUx4R3BModuleNavigationPolishSourceTest {
    @Test
    void moduleButtonsShowOnlyModuleNamesAndKeepDescriptionsAsTooltips() throws IOException {
        String navigation = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceModuleNavigation.java");
        assertTrue(navigation.contains("new ToggleButton(descriptor.title())"));
        assertTrue(navigation.contains("new Tooltip(descriptor.description())"));
        assertFalse(navigation.contains("descriptor.title() + \"\\n\" + descriptor.description()"));
    }

    @Test
    void refreshingVoiceListsDoesNotForceTheManageModule() throws IOException {
        String view = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceLibraryWorkspaceView.java");
        assertTrue(view.contains("private boolean refreshingVoiceSelectionControls"));
        assertTrue(view.contains("if (!refreshingVoiceSelectionControls)"));
        assertTrue(view.contains("refreshingVoiceSelectionControls = true"));
        assertTrue(view.contains("refreshingVoiceSelectionControls = false"));
    }

    @Test
    void moduleNavigationUsesSoberTeamsLikeSidebarWithoutGradient() throws IOException {
        String css = read("src/main/resources/css/voice-library.css");
        assertTrue(css.contains(".voice-module-navigation"));
        String navigation = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceModuleNavigation.java");
        assertTrue(css.contains("#252A44"));
        assertTrue(css.contains(".voice-module-row:selected"));
        assertTrue(css.contains("-fx-border-width: 1 1 1 4"));
        assertTrue(navigation.contains("setMaxHeight(Double.MAX_VALUE)"));
        assertTrue(navigation.contains("voice-module-navigation-spacer"));
        assertFalse(css.contains("#EAF0F8"));
        assertFalse(css.toLowerCase().contains("linear-gradient"));
    }

    private static String read(String path) throws IOException {
        return Files.readString(Path.of(path));
    }
}

package com.marcosmoreiradev.docupodcaststudio.presentation.components;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class SharedActionComponentsSourceTest {
    @Test
    void sharedActionComponentsCoverPrimarySecondaryDangerTransportRailAndMetrics() throws Exception {
        String factory = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/ActionButtonFactory.java");
        String appStyles = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/AppStyles.java");
        String actionsCss = read("src/main/resources/css/components/actions.css");
        String cardsCss = read("src/main/resources/css/components/cards.css");

        assertTrue(factory.contains("primary(String text"));
        assertTrue(factory.contains("secondary(String text"));
        assertTrue(factory.contains("danger(String text"));
        assertTrue(factory.contains("transport(String text"));
        assertTrue(factory.contains("rail(String text"));
        assertTrue(appStyles.contains("UI_ACTION_BUTTON_PRIMARY"));
        assertTrue(appStyles.contains("UI_RAIL_ACTION_BUTTON"));
        assertTrue(appStyles.contains("UI_DIAGNOSTIC_CARD"));
        assertTrue(actionsCss.contains(".ui-action-button-primary"));
        assertTrue(actionsCss.contains(".ui-transport-button"));
        assertTrue(cardsCss.contains(".ui-metric-badge"));
        assertTrue(cardsCss.contains(".ui-diagnostic-card"));
    }

    @Test
    void keyWorkspacesConsumeSharedActionCatalog() throws Exception {
        assertTrue(read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/script/ScriptWorkspaceView.java").contains("TransportControls"));
        assertTrue(read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/audio/AudioWorkspaceView.java").contains("ActionBar"));
        assertTrue(read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/storyboard/StoryboardWorkspaceView.java").contains("MetricBadge"));
        assertTrue(read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceLibraryWorkspaceView.java").contains("ActionButtonFactory"));
        assertTrue(read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/sidedock/WorkspaceSideDock.java").contains("sideDockRail"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}

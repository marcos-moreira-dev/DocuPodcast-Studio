package com.marcosmoreiradev.docupodcaststudio.presentation.components;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class GuiComponentReuseSourceTest {
    @Test
    void mainSurfacesUseSharedGuiComponentsInsteadOfAdHocJavaFxRows() throws Exception {
        String document = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java");
        String settings = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsDialog.java");
        String appStyles = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/AppStyles.java");
        String actionStrip = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/PrimaryActionStrip.java");
        String emptyState = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/EmptyStateView.java");
        String settingsPage = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/SettingsPageView.java");
        String actionFactory = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/ActionButtonFactory.java");
        String actionBar = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/ActionBar.java");
        String transport = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/TransportControls.java");
        String floating = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/FloatingReadingControlBar.java");

        assertTrue(document.contains("FloatingReadingControlBar"));
        assertTrue(document.contains("EmptyStateView"));
        assertTrue(settings.contains("SettingsPageView"));
        assertTrue(appStyles.contains("ui-primary-action-strip"));
        assertTrue(appStyles.contains("ui-action-button-primary"));
        assertTrue(appStyles.contains("ui-transport-controls"));
        assertTrue(appStyles.contains("ui-metric-badge"));
        assertTrue(appStyles.contains("ui-floating-reading-control"));
        assertTrue(actionStrip.contains("AppStyles.UI_PRIMARY_ACTION_STRIP"));
        assertTrue(floating.contains("PrimaryActionStrip"));
        assertTrue(floating.contains("TransportControls"));
        assertTrue(floating.contains("ActionButtonFactory.transportIcon"));
        assertTrue(emptyState.contains("AppStyles.UI_EMPTY_STATE"));
        assertTrue(settingsPage.contains("SectionHeader"));
        assertTrue(actionFactory.contains("ActionButtonFactory"));
        assertTrue(actionBar.contains("AppStyles.UI_ACTION_BAR"));
        assertTrue(transport.contains("ActionButtonFactory.transport"));
        assertTrue(read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentAudioNarrationPanel.java").contains("ActionButtonFactory"));
        assertTrue(read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentImageContextPanel.java").contains("ActionButtonFactory"));
        assertTrue(read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentContextDetailsPanel.java").contains("SectionHeader"));
    }

    @Test
    void workspacesDoNotHardcodeRepeatedActionButtons() throws Exception {
        List<String> workspaceSources = List.of(
                "src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/script/ScriptWorkspaceView.java",
                "src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/audio/AudioWorkspaceView.java",
                "src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/storyboard/StoryboardWorkspaceView.java",
                "src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceLibraryWorkspaceView.java",
                "src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentReadingProfilePanel.java",
                "src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentLayerRailView.java",
                "src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentActionsPanel.java",
                "src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentStructurePanel.java",
                "src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentAudioNarrationPanel.java",
                "src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentImageContextPanel.java",
                "src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentContextDetailsPanel.java",
                "src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/sidedock/WorkspaceSideDock.java"
        );
        for (String sourcePath : workspaceSources) {
            String source = read(sourcePath);
            assertFalse(source.contains("new Button("), sourcePath + " debe usar ActionButtonFactory/componentes compartidos para acciones repetibles.");
            assertTrue(source.contains("ActionButtonFactory") || source.contains("PrimaryActionStrip") || source.contains("TransportControls") || source.contains("SectionHeader") || source.contains("InfoBadge"),
                    sourcePath + " debe depender de un componente/fábrica GUI transversal.");
        }
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}

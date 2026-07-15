package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class FloatingReadingControlSourceTest {
    @Test
    void documentWorkspaceUsesFloatingGlobalReadingControlWithSharedComponents() throws Exception {
        String workspace = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java"));
        String component = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/FloatingReadingControlBar.java"));
        String css = Files.readString(Path.of("src/main/resources/css/components/actions.css"));

        assertTrue(workspace.contains("StackPane readingStage = new StackPane(documentScroll, readingControls)"));
        assertTrue(workspace.contains("StackPane.setAlignment(readingControls, Pos.TOP_CENTER)"));
        assertTrue(workspace.contains("new FloatingReadingControlBar"));
        assertTrue(workspace.contains("runPrimaryActionFromPlaybar"));
        assertTrue(workspace.contains("saveProjectBeforeAudioRequest.getAsBoolean()"));
        assertTrue(workspace.contains("viewModel::pausePlayback"));
        assertTrue(workspace.contains("viewModel::resumePlayback"));
        assertTrue(workspace.contains("viewModel::stopPlayback"));
        assertTrue(component.contains("PrimaryActionStrip"));
        assertTrue(component.contains("setMaxHeight(Region.USE_PREF_SIZE)"));
        assertTrue(component.contains("document-reading-glass-bar"));
        assertTrue(component.contains("TransportControls"));
        assertTrue(component.contains("ActionButtonFactory.transportIcon"));
        assertTrue(component.contains("primaryAction, false"));
        assertTrue(css.contains("ui-floating-reading-control"));
        assertTrue(css.contains("rgba(31, 41, 55, 0.42)"));
    }
}

package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class DocumentVisualSelectionStabilitySourceTest {
    @Test
    void imageSideDockClicksDoNotClearThePinnedVisualFragment() throws Exception {
        String workspace = Files.readString(Path.of(
                "src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java"));

        assertTrue(workspace.contains("targetInsideWorkspaceSideDock(event.getTarget())"));
        assertTrue(workspace.contains("current instanceof WorkspaceSideDock"));
        assertTrue(workspace.contains("|| targetInsideWorkspaceSideDock(event.getTarget()))"));
    }

    @Test
    void textualSelectionPinsAndRevealsTheExactVisualUnit() throws Exception {
        String viewModel = Files.readString(Path.of(
                "src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java"));
        String rail = Files.readString(Path.of(
                "src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentMediaRailView.java"));
        String panel = Files.readString(Path.of(
                "src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentImageContextPanel.java"));

        assertTrue(viewModel.contains("visualFragmentForSelection(range.blockId(), range)"));
        assertTrue(viewModel.contains("visual.ifPresent(this::pinVisualFragment)"));
        assertTrue(rail.contains("revealSelectedVisualFragment"));
        assertTrue(rail.contains("storyboardItems.getSelectionModel().select(index)"));
        assertTrue(rail.contains("storyboardItems.scrollTo(index)"));
        assertTrue(panel.contains("selectedVisualFragmentKeyProperty().addListener"));
    }
}

package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MiniMediaRailRealAssignmentsSourceTest {
    @Test
    void miniRailShowsSavedAssignmentsImagesAndUnassignedMediaInOnePlace() throws Exception {
        String mediaRail = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentMediaRailView.java"));
        String viewModel = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java"));
        String css = Files.readString(Path.of("src/main/resources/css/components/media-rail.css"));

        assertFalse(mediaRail.contains("Asignadas"));
        assertFalse(mediaRail.contains("renderAssignedMedia"));
        assertFalse(mediaRail.contains("assignedMediaCard"));
        assertTrue(mediaRail.contains("Fragmentos visuales"));
        assertFalse(mediaRail.contains("railTitle(\"Imágenes\")"));
        assertTrue(mediaRail.contains("selectDocumentFragmentRailItem"));
        assertFalse(mediaRail.contains("selectLooseStoryboardImage"));
        assertTrue(viewModel.contains("selectDocumentRangeForSegment"));
        assertTrue(css.contains("document-media-image-unassigned"));
    }
}

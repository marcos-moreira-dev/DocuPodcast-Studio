package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class BrainNarrativeLayerCoordinatorSourceTest {
    @Test
    void narrativeLayerBrainMovesOutOfShellAndKeepsDocumentAsRoot() throws Exception {
        String coordinator = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/NarrativeLayerCoordinator.java");
        String shell = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java");
        String roadmap = read("docs/productizacion/ROADMAP_POST_T65_NARRATIVE_LAYERS.md");

        assertTrue(coordinator.contains("class NarrativeLayerCoordinator"));
        assertTrue(coordinator.contains("project-side layers anchored to the narrated document"));
        assertTrue(coordinator.contains("NarrativeLayerAssignmentPolicy.conflicts"));
        assertTrue(coordinator.contains("scriptRangeForLayer"));
        assertTrue(coordinator.contains("removePrimaryAssignment"));
        assertTrue(coordinator.contains("presentations"));
        assertTrue(coordinator.contains("real target resolution"));
        assertTrue(coordinator.contains("NarrativeLayerTargetResolver"));
        assertTrue(coordinator.contains("missingTarget"));

        assertTrue(shell.contains("private final NarrativeLayerCoordinator narrativeLayerWorkflow"));
        assertTrue(shell.contains("new NarrativeLayerCoordinator"));
        assertTrue(shell.contains("narrativeLayerWorkflow.assign"));
        assertTrue(shell.contains("narrativeLayerWorkflow.removePrimaryAssignment"));
        assertTrue(shell.contains("narrativeLayerWorkflow.findAssignment"));
        assertTrue(shell.contains("narrativeLayerWorkflow.presentations"));

        assertTrue(roadmap.contains("T66"));
        assertTrue(roadmap.contains("round-trip funcional"));
    }

    private static String read(String path) throws Exception {
        Path file = Path.of(path);
        assertTrue(Files.exists(file), path + " debe existir");
        return Files.readString(file);
    }
}

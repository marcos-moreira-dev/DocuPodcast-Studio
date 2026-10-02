package com.marcosmoreiradev.docupodcaststudio.presentation.export;

import com.marcosmoreiradev.docupodcaststudio.application.export.ProjectExportTargetCatalog;
import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectMode;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ExportExecutionRouteContractTest {
    @Test
    void everyVisibleCreativeTargetHasOneCommandControllerOperationAndArtifact() {
        ProjectExportTargetCatalog catalog = new ProjectExportTargetCatalog();
        Set<com.marcosmoreiradev.docupodcaststudio.application.export.ExportableArtifactKind> visible =
                new HashSet<>();
        Arrays.stream(ProjectMode.values()).forEach(mode -> visible.addAll(catalog.creativeTargets(mode)));
        var routes = ExportCenterCoordinator.executionRoutes();

        assertEquals(visible, routes.stream().map(ExportExecutionRoute::target).collect(java.util.stream.Collectors.toSet()));
        assertEquals(routes.size(), routes.stream().map(ExportExecutionRoute::command).distinct().count());
        assertEquals(routes.size(), routes.stream().map(ExportExecutionRoute::operation).distinct().count());
        assertTrue(routes.stream().allMatch(route -> ExportController.supportedOperations().contains(route.operation())));
        assertTrue(routes.stream().allMatch(route -> route.artifactExtension().equals(".wav")
                || route.artifactExtension().equals(".mp4")));
    }
}

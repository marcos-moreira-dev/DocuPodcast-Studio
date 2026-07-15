package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class ProjectExportBlockersHf1SourceTest {
    @Test
    void workflowBlocksKnownUnavailableExportsBeforeStartingLongOperations() throws Exception {
        String workflow = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/ExportWorkflowCoordinator.java"));

        assertTrue(workflow.contains("ApplicationPreconditionException"));
        assertTrue(workflow.contains("ensureExportable(session, projectFile, script, storyboard, jobs, ExportableArtifactKind.SIMPLE_VIDEO_PACKAGE"));
        assertTrue(workflow.contains("ensureFinalVideoReady(session, projectFile, script, storyboard, jobs)"));
        assertTrue(workflow.contains("String.join(\" \", item.missingRequirements())"));
        assertTrue(workflow.contains("No se puede exportar"));
    }
}

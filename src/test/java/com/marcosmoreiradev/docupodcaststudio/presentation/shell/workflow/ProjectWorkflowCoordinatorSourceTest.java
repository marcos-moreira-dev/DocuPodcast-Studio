package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ProjectWorkflowCoordinatorSourceTest {
    @Test
    void viewModelDelegatesProjectOpenSaveAndCloseToCoordinator() throws Exception {
        String viewModel = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java"));
        assertTrue(viewModel.contains("private final ProjectWorkflowCoordinator projectWorkflow"));
        assertTrue(viewModel.contains("projectWorkflow.openProject(sourceFile)"));
        assertTrue(viewModel.contains("projectWorkflow.saveProject(session, targetFile"));
        assertTrue(viewModel.contains("projectWorkflow.closeProject()"));
        assertFalse(viewModel.contains("loadProjectWorkspaceArtifacts().load(sourceFile)"));
        assertFalse(viewModel.contains("validateProjectWorkspaceIntegrity()\n                .validate(project, sourceFile, hydration)"));
    }

    @Test
    void coordinatorOwnsProjectWorkflowAndHydrationSequence() throws Exception {
        String coordinator = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/ProjectWorkflowCoordinator.java"));
        assertTrue(coordinator.contains("public OpenedProjectContext openProject(Path sourceFile)"));
        assertTrue(coordinator.contains("validateProjectPayload().validate(project).throwIfInvalid()"));
        assertTrue(coordinator.contains("loadProjectWorkspaceArtifacts().load(sourceFile)"));
        assertTrue(coordinator.contains("validateProjectWorkspaceIntegrity()"));
        assertTrue(coordinator.contains("session::hydrateImportedDocument"));
        assertTrue(coordinator.contains("public void saveProject(ProjectSession session"));
    }
}

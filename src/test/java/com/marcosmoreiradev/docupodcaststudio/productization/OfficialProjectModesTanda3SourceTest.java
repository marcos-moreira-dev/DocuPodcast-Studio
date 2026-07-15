package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tanda 3: official project modes are visible product modes over legacy ProjectKind. */
final class OfficialProjectModesTanda3SourceTest {
    @Test
    void projectCreationDialogSelectsOfficialModeWithoutReplacingProjectKind() throws Exception {
        String dialog = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/dialogs/ProjectNameDialog.java");
        String shell = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java");
        String useCase = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/project/CreateProjectUseCase.java");
        String metadata = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/domain/project/ProjectMetadata.java");

        assertTrue(dialog.contains("ComboBox<ProjectMode>"));
        assertTrue(dialog.contains("ProjectMode.officialModes()"));
        assertTrue(dialog.contains("ProjectMode.defaultMode()"));
        assertTrue(shell.contains("projectNameDialog.showSetup(owner())"));
        assertTrue(shell.contains("viewModel.createNewProject(setup.title(), setup.mode())"));
        assertTrue(useCase.contains("create(String title, ProjectMode mode)"));
        assertTrue(metadata.contains("ProjectKind kind"));
        assertTrue(metadata.contains("ProjectMode mode"));
    }

    @Test
    void commandPolicyOwnsModeVisibilityForMenuAndRibbon() throws Exception {
        String policy = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/command/CommandAvailabilityPolicy.java");
        String shell = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java");
        String ribbon = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/ribbon/RibbonView.java");

        assertTrue(policy.contains("visibleBinding(AppCommandId commandId, DocuPodcastShellViewModel viewModel)"));
        assertTrue(policy.contains("ProjectModeCapabilities.forMode"));
        assertTrue(policy.contains("OPEN_THEATRE_SCRIPT"));
        assertTrue(policy.contains("IMPORT_IMAGE_FOR_SELECTION"));
        assertTrue(shell.contains("item.visibleProperty().bind(commandAvailabilityPolicy.visibleBinding(commandId, viewModel))"));
        assertTrue(ribbon.contains("button.visibleProperty().bind(commandAvailabilityPolicy.visibleBinding(command.commandId(), viewModel))"));
        assertFalse(policy.contains("OPEN_STORYBOARD ->"));
    }

    @Test
    void fragmentProjectionIsRegisteredAsApplicationService() throws Exception {
        String services = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/ApplicationServices.java");
        String factory = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/bootstrap/ApplicationServicesFactory.java");
        String fragmentServices = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/services/FragmentApplicationServices.java");
        String coordinator = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/fragment/FragmentWorkspaceProjectionCoordinator.java");

        assertTrue(services.contains("FragmentApplicationServices fragment"));
        assertTrue(factory.contains("new FragmentWorkspaceProjectionCoordinator(buildFragmentProjection)"));
        assertTrue(fragmentServices.contains("BuildFragmentWorkspaceProjectionUseCase"));
        assertTrue(coordinator.contains("FragmentWorkspaceSummary"));
        assertTrue(coordinator.contains("PlaybackManifest"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}

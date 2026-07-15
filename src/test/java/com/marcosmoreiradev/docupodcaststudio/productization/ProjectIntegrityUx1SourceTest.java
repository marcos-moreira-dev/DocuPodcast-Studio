package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** PROJECT-INTEGRITY-UX1 ensures integrity findings are visible decisions, not silent status text. */
final class ProjectIntegrityUx1SourceTest {
    @Test
    void projectIntegrityWarningsOpenMessageBoxFromShell() throws Exception {
        String shell = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java");
        String viewModel = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java");
        String coordinator = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/ProjectWorkflowCoordinator.java");
        String factory = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/project/ProjectIntegrityUserDecisionFactory.java");

        assertTrue(shell.contains("handleInspectProjectIntegrity"));
        assertTrue(shell.contains("alertPresenter.showDecision(decision, owner())"));
        assertTrue(shell.contains("viewModel.inspectProjectIntegrityDecision()"));
        assertTrue(viewModel.contains("inspectProjectIntegrityDecision"));
        assertTrue(coordinator.contains("ProjectIntegrityUserDecisionFactory.fromReport(report)"));
        assertTrue(factory.contains("Proyecto abierto con advertencias"));
        assertTrue(factory.contains("Proyecto requiere reparación"));
    }

    private static String read(String path) throws Exception { return Files.readString(Path.of(path)); }
}

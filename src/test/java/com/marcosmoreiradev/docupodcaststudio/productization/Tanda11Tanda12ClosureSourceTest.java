package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class Tanda11Tanda12ClosureSourceTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();

    @Test
    void processKindsAndDashboardCloseTheJobsContract() throws Exception {
        String kinds = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/domain/process/ProcessJobKind.java");
        String services = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/services/ProcessApplicationServices.java");
        String dashboard = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/process/BuildProcessJobDashboardProjectionUseCase.java");

        assertTrue(kinds.contains("MANAGED_DOWNLOAD"));
        assertTrue(kinds.contains("VISUAL_GENERATION"));
        assertTrue(kinds.contains("FINAL_EXPORT"));
        assertTrue(services.contains("BuildProcessJobDashboardProjectionUseCase buildProcessJobDashboardProjection"));
        assertTrue(dashboard.contains("missingOutputDiagnostics"));
        assertTrue(dashboard.contains("ProcessJobRecoveryAction.retry"));
    }

    @Test
    void exportCenterShowsRelatedProcessState() throws Exception {
        String state = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/export/ExportCenterState.java");
        String target = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/export/ExportTargetPresentation.java");
        String dialog = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/export/ExportCenterDialog.java");
        String coordinator = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/export/ExportCenterCoordinator.java");

        assertTrue(state.contains("List<ProcessJobSnapshot> processJobs"));
        assertTrue(target.contains("String relatedProcessSummary"));
        assertTrue(dialog.contains("Procesos"));
        assertTrue(coordinator.contains("listProcessJobs().listPersisted"));
    }

    @Test
    void finalArchitectureGuardrailsRemainInPlace() throws Exception {
        String registry = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/command/AppCommandRegistry.java");
        String ribbon = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/ribbon/RibbonDefinitionCatalog.java");
        String checklist = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/runtime/RunMigrationClosureChecklistUseCase.java");
        long viewModelLines = Files.lines(ROOT.resolve("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java")).count();

        assertTrue(viewModelLines <= 2600, "DocuPodcastShellViewModel must stay <= 2600 lines");
        assertTrue(registry.contains("hidden(AppCommandId.OPEN_STORYBOARD"));
        assertTrue(registry.contains("hidden(AppCommandId.OPEN_AUDIO_JOBS"));
        assertFalse(ribbon.contains("cmd(AppCommandId.OPEN_STORYBOARD"));
        assertFalse(ribbon.contains("cmd(AppCommandId.OPEN_AUDIO_JOBS"));
        assertTrue(checklist.contains("youtube"));
        assertTrue(checklist.contains("whisper"));
    }

    @Test
    void componentInventoryIncludesClosingComponents() throws Exception {
        String catalog = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/GuiComponentCatalog.java");
        String inventory = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/BuildGuiComponentInventoryUseCase.java");

        assertTrue(catalog.contains("ExportCenterDialog"));
        assertTrue(catalog.contains("NarrativeVisualProductionWorkspaceView"));
        assertTrue(catalog.contains("AudioInputDeviceSelector"));
        assertTrue(catalog.contains("OperationalStatusStrip"));
        assertTrue(inventory.contains("especializado-narrativo"));
        assertTrue(inventory.contains("legacy-interno"));
    }

    private static String read(String relativePath) throws Exception {
        return Files.readString(ROOT.resolve(relativePath));
    }
}

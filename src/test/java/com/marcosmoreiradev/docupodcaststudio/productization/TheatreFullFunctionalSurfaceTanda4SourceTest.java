package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tanda 4: protect the complete theatre surface before transversal extraction work. */
final class TheatreFullFunctionalSurfaceTanda4SourceTest {
    @Test
    void theatreModeCommandsWorkspacesAndAvailabilityStayConnected() throws IOException {
        String commands = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/command/AppCommandId.java");
        String registry = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/command/AppCommandRegistry.java");
        String availability = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/command/CommandAvailabilityPolicy.java");
        String shell = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java");
        String workspaces = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/workspace/WorkspaceKind.java");

        assertTrue(commands.contains("OPEN_THEATRE_SCRIPT"));
        assertTrue(commands.contains("OPEN_THEATRE_IMAGE_GENERATION"));
        assertTrue(commands.contains("IMPORT_THEATRE_GRAMMAR"));
        assertTrue(commands.contains("EXPORT_THEATRE_GRAMMAR_TEMPLATE"));
        assertTrue(commands.contains("EXPORT_THEATRE_WORK"));
        assertTrue(commands.contains("EXPORT_THEATRE_SPATIAL_VIEW"));
        assertTrue(commands.contains("EXPORT_THEATRE_PORTION"));
        assertTrue(registry.contains("Gestionar frames de la obra"));
        assertTrue(availability.contains("capabilities.theatreProduction()"));
        assertTrue(shell.contains("WorkspaceKind.THEATRE_SCRIPT"));
        assertTrue(shell.contains("new TheatreImageGenerationWorkspaceView(viewModel)"));
        assertTrue(workspaces.contains("THEATRE_SCRIPT"));
        assertTrue(workspaces.contains("THEATRE_IMAGE_GENERATION"));
    }

    @Test
    void theatreScriptDockKeepsAllDedicatedModules() throws IOException {
        String dock = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/theatre/TheatreSideDock.java");
        String moduleIds = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/sidedock/SideDockModuleId.java");
        String document = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java");

        assertTrue(document.contains("DocumentWorkspaceMode.THEATRE_SCRIPT"));
        assertTrue(document.contains("buildTheatreSideDock()"));
        assertTrue(dock.contains("THEATRE_FRAGMENT_IMAGES"));
        assertTrue(dock.contains("THEATRE_CHARACTERS"));
        assertTrue(dock.contains("THEATRE_TEXTUAL_MAP"));
        assertTrue(dock.contains("THEATRE_SPATIAL_MAP"));
        assertTrue(dock.contains("THEATRE_OBJECTS"));
        assertTrue(dock.contains("THEATRE_ACTIONS is kept as a legacy id"));
        assertTrue(dock.contains("DocumentImageContextPanel"));
        assertTrue(dock.contains("DocumentMediaRailView"));
        assertTrue(moduleIds.contains("THEATRE_ACTIONS"));
        assertFalse(dock.contains("SideDockModuleId.DOCUMENT_IMAGE"));
    }

    @Test
    void theatreGrammarPresentationLayerRemainsCompatibilityFacade() throws IOException {
        String parser = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/theatre/TheatreGrammarMarkdownParser.java");
        String template = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/theatre/TheatreGrammarTemplate.java");
        String applicationParser = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/theatre/grammar/TheatreGrammarMarkdownParser.java");
        String applicationTemplate = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/theatre/grammar/TheatreGrammarTemplate.java");

        assertTrue(parser.contains("application.theatre.grammar.TheatreGrammarMarkdownParser.parse"));
        assertTrue(template.contains("application.theatre.grammar.TheatreGrammarTemplate.markdown"));
        assertTrue(applicationParser.contains("public final class TheatreGrammarMarkdownParser"));
        assertTrue(applicationTemplate.contains("public final class TheatreGrammarTemplate"));
        assertFalse(parser.contains("private static final class ActBuilder"));
        assertFalse(parser.contains("private static final class SceneBuilder"));
    }

    @Test
    void theatreAiWorkspaceKeepsEngineGenerateJobsAndContextFlows() throws IOException {
        String view = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/theatre/TheatreImageGenerationWorkspaceView.java");
        String modules = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/theatre/TheatreAiModuleId.java");
        String viewModel = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java");
        String frameWorkflow = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/TheatreFrameGenerationWorkflow.java");
        String imageWorkflow = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/TheatreImageGenerationWorkflow.java");

        assertTrue(modules.contains("HOME"));
        assertTrue(modules.contains("ENGINE"));
        assertTrue(modules.contains("GENERATE"));
        assertTrue(modules.contains("JOBS"));
        assertTrue(view.contains("runLocalTheatreImageSmoke"));
        assertTrue(view.contains("theatreImageGenerationQueue"));
        assertTrue(view.contains("generateTheatreImageCandidate"));
        assertTrue(view.contains("approveSelectedImageCandidate"));
        assertTrue(view.contains("generateTheatreFrames"));
        assertTrue(view.contains("approveSelectedFrameCandidate"));
        assertTrue(view.contains("exportTheatreContextPackages"));
        assertTrue(view.contains("exportTheatreInterventionContext"));
        assertTrue(view.contains("promptDrafts"));
        assertTrue(viewModel.contains("approveTheatreGeneratedFrameCandidate"));
        assertTrue(frameWorkflow.contains("TheatreFrameGenerationRequest"));
        assertTrue(imageWorkflow.contains("TheatreGeneratedImageCandidate"));
    }

    @Test
    void theatreExportVariantsStayInventoryAndWorkExportUsesDedicatedComposer() throws IOException {
        String kinds = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/export/ExportableArtifactKind.java");
        String readiness = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/export/InspectExportReadinessUseCase.java");
        String center = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/export/ExportCenterCoordinator.java");
        String shell = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java");
        String exportWorkflow = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/ExportWorkflowCoordinator.java");
        String theatreVideo = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/theatre/ExportTheatreVideoUseCase.java");
        String workPlan = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/video/BuildTheatreCleanVideoPlanUseCase.java");
        String portion = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/theatre/TheatrePortionExportOptions.java");
        String doc = read("DOCUMENTACION_ACTUAL/TANDA_04_REVISION_INTEGRAL_TEATRO/TEATRO_FUNCIONAL_COMPLETO.md");

        assertTrue(kinds.contains("THEATRE_WORK_VIDEO"));
        assertTrue(kinds.contains("THEATRE_SPATIAL_MAP_VIDEO"));
        assertTrue(kinds.contains("THEATRE_PORTION_VIDEO"));
        assertTrue(readiness.contains("theatreWork(projectFile, theatre)"));
        assertTrue(readiness.contains("theatreSpatialMap(projectFile, theatre)"));
        assertTrue(readiness.contains("theatrePortion(projectFile, theatre)"));
        assertTrue(center.contains("THEATRE_WORK_VIDEO"));
        assertTrue(center.contains("THEATRE_SPATIAL_MAP_VIDEO"));
        assertTrue(center.contains("THEATRE_PORTION_VIDEO"));
        assertTrue(shell.contains("handleExportTheatreWork"));
        assertTrue(shell.contains("handleExportTheatreSpatialView"));
        assertTrue(shell.contains("handleExportTheatrePortion"));
        assertTrue(shell.contains("exportTheatreWorkInBackground"));
        assertTrue(exportWorkflow.contains("exportTheatreWork("));
        assertTrue(theatreVideo.contains("exportWork("));
        assertTrue(workPlan.contains("BuildTheatreCleanVideoPlanUseCase"));
        assertFalse(workPlan.contains("TheatreWorkVideoOptions"));
        assertTrue(portion.contains("CLEAN_VIDEO"));
        assertTrue(portion.contains("THEATRE_MAP"));
        assertTrue(doc.contains("`EXPORT_THEATRE_WORK` | `THEATRE_WORK_VIDEO`"));
        assertTrue(doc.contains("compositor teatral dedicado"));
    }

    private static String read(String path) throws IOException {
        return Files.readString(Path.of(path));
    }
}

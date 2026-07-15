package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Theatre AI generation must be a modular workspace, not one crowded panel. */
final class TheatreAiGenerationModularWorkspaceSourceTest {
    @Test
    void theatreAiWorkspaceUsesVoiceLikeModularShell() throws IOException {
        String view = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/theatre/TheatreImageGenerationWorkspaceView.java");
        String ids = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/theatre/TheatreAiModuleId.java");
        assertTrue(view.contains("TheatreAiWorkspaceShell"));
        assertTrue(view.contains("TheatreAiModuleNavigation"));
        assertTrue(ids.contains("HOME"));
        assertTrue(ids.contains("ENGINE"));
        assertTrue(ids.contains("GENERATE"));
        assertTrue(ids.contains("JOBS"));
        assertFalse(ids.contains("BATCHES"));
        assertFalse(ids.contains("RESULTS"));
        assertFalse(ids.contains("CONTEXTS"));
        assertFalse(ids.contains("FRAMES"));
        assertFalse(view.contains("SplitPane"));
    }

    @Test
    void theatreAiWorkspaceUsesLocalEngineLanguageAndKeepsComfyAsInternalDetail() throws IOException {
        String view = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/theatre/TheatreImageGenerationWorkspaceView.java");
        String command = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/command/AppCommandRegistry.java");
        String workspace = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/workspace/WorkspaceDescriptorCatalog.java");
        assertTrue(view.contains("Motor local de imagen IA"));
        assertTrue(view.contains("runLocalTheatreImageSmoke"));
        assertFalse(view.contains("Endpoint local"));
        assertFalse(view.contains("localEndpoint"));
        assertFalse(view.contains("http://127.0.0.1"));
        assertTrue(command.contains("motor local de imagen IA"));
        assertTrue(workspace.contains("motor local"));
        assertFalse(command.contains("cola ComfyUI"));
        assertFalse(workspace.contains("cola ComfyUI"));
    }

    @Test
    void framesModuleNamesStopMotionAsIntermediateBetweenInterventions() throws IOException {
        String view = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/theatre/TheatreImageGenerationWorkspaceView.java");
        String workflow = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/TheatreFrameGenerationWorkflow.java");
        assertTrue(view.contains("intermedios crea continuidad entre intervenciones consecutivas"));
        assertTrue(view.contains("Trabajos"));
        assertFalse(view.contains("Que generar"));
        assertFalse(view.contains("Lotes"));
        assertTrue(workflow.contains("transitionUnit"));
        assertTrue(workflow.contains("Frame intermedio de continuidad"));
        assertTrue(workflow.contains("supportsIntermediateGeneration"));
        assertTrue(workflow.contains("source-target-reference"));
    }

    @Test
    void generateModuleCombinesContextPromptAndReadOnlyNavigator() throws IOException {
        String view = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/theatre/TheatreImageGenerationWorkspaceView.java");
        String navigator = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/theatre/TheatreInterventionNavigator.java");
        assertTrue(view.contains("TheatreInterventionNavigator"));
        assertTrue(view.contains("Contexto y frames"));
        assertTrue(view.contains("Frames de la intervencion seleccionada"));
        assertTrue(view.contains("Contexto limpio para frames"));
        assertTrue(view.contains("Frame principal"));
        assertTrue(view.contains("Frame intermedio"));
        assertTrue(view.contains("Texto del contexto"));
        assertTrue(view.contains("Disposicion espacial configurada"));
        assertTrue(view.contains("theatre-ai-frame-carousel"));
        assertTrue(view.contains("Referencias que se aplicaran al motor"));
        assertFalse(view.contains("\\nIntervencion: "));
        assertTrue(view.contains("Clic derecho sobre una intervención para procesarla o exportar su paquete IA."));
        assertTrue(view.contains("Paquetes IA"));
        assertFalse(view.contains("Instrucciones para la imagen"));
        assertFalse(view.contains("Restaurar prompt base"));
        assertTrue(view.contains("promptDrafts"));
        assertFalse(view.contains("Generar alcance"));
        assertTrue(view.contains("TheatreImageGenerationJob"));
        assertFalse(view.contains("contextsModule"));
        assertTrue(navigator.contains("Read-only"));
        assertTrue(navigator.contains("Procesar acto"));
        assertTrue(navigator.contains("Procesar escena"));
        assertTrue(navigator.contains("TheatreWorkspaceEmptyState.noActsMessage"));
        assertTrue(navigator.contains("TheatreWorkspaceEmptyState.noScenesMessage"));
        assertTrue(navigator.contains("TheatreWorkspaceEmptyState.interventionSequenceMessage"));
        assertTrue(navigator.contains("TheatreWorkspaceEmptyState.preparationNotice"));
        assertTrue(navigator.contains("TheatreInterventionSelectionBridge.bind"));
        assertTrue(navigator.contains("TheatreInterventionSelectionBridge.blockSelector(viewModel, scene)"));
        assertTrue(read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/theatre/TheatreTextSequenceCanvas.java")
                .contains("Procesar intervención"));
        assertFalse(navigator.contains("Agregar escena"));
        assertFalse(navigator.contains("Nuevo acto"));
    }

    @Test
    void cleanFrameContextDoesNotFeedCurrentFragmentOrSpatialMapAsReferences() throws IOException {
        String view = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/theatre/TheatreImageGenerationWorkspaceView.java");
        String workflow = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/TheatreImageGenerationWorkflow.java");
        String contextBuilder = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/theatre/BuildTheatreVisualGenerationContextUseCase.java");
        String generationContext = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/theatre/TheatreVisualGenerationContext.java");
        String frameWorkflow = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/TheatreFrameGenerationWorkflow.java");
        assertFalse(workflow.contains("\"fragmento visual\", \"Fragmento \" + unit.interventionId()"));
        assertFalse(workflow.contains("\"mapa espacial\", scene.displayName()"));
        assertTrue(workflow.contains("BuildTheatreVisualGenerationContextUseCase"));
        assertTrue(contextBuilder.contains("addParticipantImage"));
        assertTrue(contextBuilder.contains("\"personaje\""));
        assertTrue(contextBuilder.contains("\"objeto\""));
        assertTrue(contextBuilder.contains("\"teatro vacio\""));
        assertTrue(contextBuilder.contains("Fondo de escenario - "));
        assertFalse(contextBuilder.contains("continuidad anterior"));
        assertFalse(contextBuilder.contains("continuidad siguiente"));
        assertFalse(contextBuilder.contains("neighbor("));
        assertFalse(generationContext.contains("VisualConditioningRole.PREVIOUS_FRAME"));
        assertFalse(generationContext.contains("VisualConditioningRole.NEXT_FRAME"));
        assertTrue(view.contains("backdropInstructionForDisplay"));
        assertTrue(view.contains("Fondo de escenario: usa"));
        assertTrue(view.contains("documentMediaRevisionProperty().addListener"));
        assertTrue(view.contains("refreshAfterMediaRevision"));
        assertTrue(frameWorkflow.contains("!normalize(unit.fullText()).isBlank()"));
        assertTrue(frameWorkflow.contains("!normalize(unit.spatialContextText()).isBlank()"));
        assertTrue(workflow.contains("spatial blocking context"));
    }

    @Test
    void frameBatchGeneratesAllPrincipalsBeforeTransitions() throws IOException {
        String workflow = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/TheatreFrameGenerationWorkflow.java");

        assertTrue(workflow.contains("boolean[] principalGenerated = new boolean[units.size()]"));
        assertTrue(workflow.indexOf("Generando frame principal") < workflow.indexOf("Generando frame intermedio"));
        assertTrue(workflow.contains("if (!principalGenerated[i] || !principalGenerated[i + 1])"));
        assertTrue(workflow.contains("unit, 1, false"));
        assertTrue(workflow.contains("2, true, next.interventionId()"));
    }

    private static String read(String path) throws IOException {
        return Files.readString(Path.of(path));
    }
}

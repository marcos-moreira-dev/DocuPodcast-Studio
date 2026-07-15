package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guards Tanda 7: visual generation is transversal and theatre only adapts context. */
final class VisualTransversalTanda7SourceTest {
    @Test
    void visualApplicationOwnsComfyUiClientAndContracts() throws Exception {
        assertTrue(Files.isRegularFile(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/visual/VisualEngineRequest.java")));
        assertTrue(Files.isRegularFile(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/visual/VisualEngineResult.java")));
        assertTrue(Files.isRegularFile(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/visual/ComfyUiVisualEngineClient.java")));

        String services = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/services/VisualProductionApplicationServices.java");
        String factory = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/bootstrap/ApplicationServicesFactory.java");

        assertTrue(services.contains("ComfyUiVisualEngineClient comfyUiVisualEngineClient"));
        assertTrue(factory.contains("ComfyUiVisualEngineClient visualEngineClient = new ComfyUiVisualEngineClient"));
        assertTrue(factory.contains("new LocalTheatreImageEngineManager("));
        assertTrue(factory.contains("visualEngineClient);"));
        assertTrue(factory.contains("new VisualProductionApplicationServices("));
        assertTrue(factory.contains("visualEngineClient,"));
    }

    @Test
    void theatreWorkflowsDelegateHttpGenerationToVisualClient() throws Exception {
        String theatreGeneration = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/TheatreImageGenerationWorkflow.java");
        String frameGeneration = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/TheatreFrameGenerationWorkflow.java");

        assertTrue(theatreGeneration.contains("visualEngineClient.generate"));
        assertTrue(theatreGeneration.contains("visualEngineRequest"));
        assertTrue(theatreGeneration.contains("ImageEnginePresetSupportPolicy"));
        assertTrue(theatreGeneration.contains("contextAssets(ProjectSession session"));
        assertFalse(theatreGeneration.contains("new ComfyUiClient"));
        assertFalse(theatreGeneration.contains("HttpRequest.newBuilder"));
        assertFalse(theatreGeneration.contains("java.net.http.HttpClient"));

        assertTrue(frameGeneration.contains("visualEngineClient.generate"));
        assertTrue(frameGeneration.contains("imageWorkflow.visualEngineRequest"));
        assertFalse(frameGeneration.contains("new ComfyUiClient"));
        assertFalse(frameGeneration.contains("HttpRequest.newBuilder"));
        assertFalse(frameGeneration.contains("java.net.http.HttpClient"));
    }

    @Test
    void managerStartsEngineWithQuotedDetachedLauncherAndLongReadiness() throws Exception {
        String manager = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/modelsetup/LocalTheatreImageEngineManager.java");

        assertTrue(manager.contains("detachedLaunchCommand(command)"));
        assertTrue(manager.contains("detached.add(\"start\")"));
        assertTrue(manager.contains("detached.add(\"\\\"\\\"\")"));
        assertTrue(manager.contains("detached.addAll(command)"));
        assertTrue(manager.contains("readinessWait(current)"));
        assertTrue(manager.contains("Math.min(240, Math.max(60, seconds))"));
        assertTrue(manager.contains("ComfyUiVisualEngineClient visualEngineClient"));
        assertTrue(manager.contains("visualEngineClient.generate"));
        assertTrue(manager.contains("baseUrl="));
        assertTrue(manager.contains("launcher="));
        assertFalse(manager.contains("HttpRequest.newBuilder"));
        assertFalse(manager.contains("smokeWorkflowPayload"));
    }

    @Test
    void oldPresentationComfyUiClientWasRemovedAndDocumented() throws Exception {
        assertFalse(Files.exists(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/ComfyUiClient.java")));

        String doc = read("DOCUMENTACION_ACTUAL/TANDA_07_VISUALES_TRANSVERSALES/VISUAL_TRANSVERSAL_CONTRACT.md");
        assertTrue(doc.contains("VisualEngineRequest"));
        assertTrue(doc.contains("ComfyUiVisualEngineClient"));
        assertTrue(doc.contains("TheatreImageGenerationWorkflow"));
        assertTrue(doc.contains("respaldo"));
        assertTrue(doc.contains("no se copia codigo"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}

package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class StoryboardVideoFromRenderPlanTi3SourceTest {
    @Test
    void videoPlanHasRenderUnitEntryPointAndSilentVisualContract() throws Exception {
        String useCase = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/video/BuildSimpleVideoPlanUseCase.java"));
        assertTrue(useCase.contains("RenderUnitPlan renderUnitPlan"));
        assertTrue(useCase.contains("renderUnitPlan.videoUnits()"));
        assertTrue(useCase.contains("RenderUnitKind.VISUAL_SILENT"));
        assertTrue(useCase.contains("completedAudioBySegmentOrUnit"));
    }

    @Test
    void commandPlanSupportsSyntheticSilenceInsteadOfBlockingSilentVisuals() throws Exception {
        String commandPlan = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/video/BuildVideoRenderCommandPlanUseCase.java"));
        assertTrue(commandPlan.contains("frame.silentVisual()"));
        assertTrue(commandPlan.contains("anullsrc=channel_layout=stereo:sample_rate=44100"));
        assertTrue(commandPlan.contains("Hay frames visuales silenciosos"));
    }

    @Test
    void shellExportsStoryboardVideoFromRenderPlanBeforeLegacyFallback() throws Exception {
        String workflow = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/ExportWorkflowCoordinator.java"));
        String shell = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java"));
        assertTrue(workflow.contains("buildRenderUnitPlan()"));
        assertTrue(workflow.contains("export(session.project(), renderUnitPlan, jobs, targetDirectory)"));
        assertTrue(workflow.contains("Paquete de video simple preparado"));
        assertTrue(shell.contains("exportWorkflow.exportSimpleVideoPackage"));
    }

    @Test
    void documentationPreservesRemainingRoadmapAfterTi3() throws Exception {
        String roadmap = Files.readString(Path.of("docs/productizacion/ROADMAP_RESTANTE_POST_TI1_DETALLADO.md"));
        assertTrue(roadmap.contains("TI3"));
        assertTrue(roadmap.contains("video debe nacer de RenderUnitPlan"));
    }
}

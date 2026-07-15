package com.marcosmoreiradev.docupodcaststudio.presentation.theatre;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TheatreImageGenerationWorkspaceSourceTest {
    @Test
    void exposesAspectRatioAndSwitchesToJobsWhenGenerating() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/theatre/TheatreImageGenerationWorkspaceView.java"));

        assertTrue(source.contains("ComboBox<TheatreImageAspectRatio> aspectRatioSelector"));
        assertTrue(source.contains("row(\"Relacion de aspecto\", aspectRatioSelector)"));
        assertTrue(source.contains("TheatreImageAspectRatio.WIDE_16_9"));
        assertTrue(source.contains("activeModule.set(TheatreAiModuleId.JOBS);"));
        assertTrue(source.contains("replaceJobById(job.id()"));
    }

    @Test
    void engineModuleShowsSmokeResultInlineWithImageAndFullscreen() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/theatre/TheatreImageGenerationWorkspaceView.java"));

        assertTrue(source.contains("engineResultPanel()"));
        assertTrue(source.contains("section(\"Resultado de prueba\")"));
        assertTrue(source.contains("ImageView engineResultImage"));
        assertTrue(source.contains("ImageFullscreenViewer.show"));
        assertTrue(source.contains("AppIcon.FULLSCREEN"));
        assertTrue(source.contains("Descargar imagen"));
        assertTrue(source.contains("new FileChooser()"));
        assertTrue(source.contains("showPerformance"));
        assertTrue(source.contains("ImageEngineSmokeImageStore.outputDirectory()"));
        assertTrue(source.contains("PNG temporal listo. Usa Descargar imagen"));
        assertTrue(source.contains("PNG temporal generado"));
        assertTrue(source.contains("new ImageEngineSmokeRequest("));
        assertFalse(source.contains("section(\"Dependencias\")"));
        assertFalse(source.contains("Copiar ruta"));
        assertFalse(source.contains("engineResultOpenFolder"));
        assertFalse(source.contains("ActionButtonFactory.secondary(\"Abrir carpeta\", this::openEngineResultFolder)"));
        assertFalse(source.contains("new Dialog<>"));
        assertFalse(source.contains("dialog.setHeaderText(\"Resultado de Imagen IA teatral\")"));
    }

    @Test
    void previewFramesAreBoundedSoImageViewsDoNotExpandWorkspace() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/theatre/TheatreImageGenerationWorkspaceView.java"));

        assertTrue(source.contains("PREVIEW_COLUMN_MAX_WIDTH = 640.0"));
        assertTrue(source.contains("configurePreviewFrame(generatedPreviewFrame, generatedPreviewImage"));
        assertTrue(source.contains("configurePreviewFrame(engineResultFrame, engineResultImage"));
        assertTrue(source.contains("constrainPreviewColumn(result)"));
        assertTrue(source.contains("constrainPreviewColumn(frames)"));
        assertFalse(source.contains("generatedPreviewFrame.setMaxWidth(Double.MAX_VALUE)"));
        assertFalse(source.contains("engineResultFrame.setMaxWidth(Double.MAX_VALUE)"));
    }

    @Test
    void selectedFramesUseExplicitCarouselReprocessingInsteadOfFixedPreviews() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/theatre/TheatreImageGenerationWorkspaceView.java"));

        assertTrue(source.contains("ScrollPane selectedFrameCarouselScroll"));
        assertTrue(source.contains("HBox selectedFrameCarousel"));
        assertTrue(source.contains("Frame principal"));
        assertTrue(source.contains("Frame intermedio"));
        assertTrue(source.contains("Frame siguiente"));
        assertTrue(source.contains("setOnContextMenuRequested"));
        assertTrue(source.contains("reprocess.setDisable(!reprocessEnabled)"));
        assertTrue(source.contains("Genera primero los frames principales actual y siguiente."));
        assertTrue(source.contains("regenerateTransition(current, next)"));
        assertTrue(source.contains("FRAME_CAROUSEL_CARD_WIDTH = 270.0"));
        assertTrue(source.contains("FRAME_CAROUSEL_PREVIEW_WIDTH = 250.0"));
        assertTrue(source.contains("preview.setMaxSize(FRAME_CAROUSEL_PREVIEW_WIDTH, FRAME_CAROUSEL_PREVIEW_HEIGHT)"));
        assertTrue(source.contains("FRAME_CAROUSEL_VIEWPORT_HEIGHT = 250.0"));
        assertTrue(source.contains("selectedFrameCarouselScroll.setPrefViewportHeight(FRAME_CAROUSEL_VIEWPORT_HEIGHT)"));
        assertTrue(source.contains("\\nCharacter: \" + unit.speaker()"));
        assertFalse(source.contains("selectedInterventionImageFrame"));
        assertFalse(source.contains("selectedTransitionFrame"));
    }

    @Test
    void contextTextCanBeEditedSavedAndUsedAsPromptBase() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/theatre/TheatreImageGenerationWorkspaceView.java"));
        String viewModel = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java"));
        String policy = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/theatre/TheatreContextTextPolicy.java"));

        assertTrue(source.contains("contextPreview.setEditable(true)"));
        assertTrue(source.contains("Guardar contexto textual"));
        assertTrue(source.contains("saveContextText"));
        assertTrue(source.contains("promptFor(unit)"));
        assertTrue(viewModel.contains("saveTheatreContextText"));
        assertTrue(viewModel.contains("theatreContextTextOverride"));
        assertTrue(policy.contains("theatre.contextText."));
    }

    @Test
    void packageExportActionsExplainTheirScopeWithTooltips() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/theatre/TheatreImageGenerationWorkspaceView.java"));

        assertTrue(source.contains("Guarda el contexto de IA de la intervencion seleccionada"));
        assertTrue(source.contains("Guarda un paquete de contexto por cada intervencion disponible"));
    }

    @Test
    void generateModuleCanBatchGenerateAndCancelIntermediateFramesInsideProject() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/theatre/TheatreImageGenerationWorkspaceView.java"));
        String workflow = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/TheatreImageGenerationWorkflow.java"));
        String client = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/visual/ComfyUiVisualEngineClient.java"));

        assertTrue(source.contains("Button intermediateBatchButton"));
        assertTrue(source.contains("Generar frames intermedios"));
        assertTrue(source.contains("Detener generacion de frames intermedios"));
        assertTrue(source.contains("Generar faltantes"));
        assertTrue(source.contains("Generar todos / sobrescribir"));
        assertTrue(source.contains("Probar primer par"));
        assertTrue(source.contains("IntermediateBatchMode.OVERWRITE_ALL"));
        assertTrue(source.contains("ensureIntermediateFrameEngineAvailable"));
        assertTrue(source.contains("toggleIntermediateFrameGeneration"));
        assertTrue(source.contains("TheatreIntermediateFrameBatchPlanner"));
        assertTrue(source.contains("generateTheatreRifeIntermediateFrameCandidate"));
        assertTrue(source.contains("resolve(\"generated\").resolve(\"teatro-ia\").resolve(\"intermedios\")"));
        assertTrue(source.contains("approveTheatreGeneratedFrameCandidate(candidate)"));
        assertTrue(source.contains("List.of(\"PREVIOUS_FRAME\", \"NEXT_FRAME\")"));
        assertTrue(workflow.contains("generated/teatro-ia/intermedios/rife"));
        assertTrue(workflow.contains("interpolateMiddleFrame"));
        assertTrue(client.contains("FrameInterpolationModelLoader"));
        assertTrue(client.contains("FrameInterpolate"));
        assertFalse(source.contains("Generar posibles"));
        assertFalse(source.contains("Generar frames intermedios\", () -> processScope"));
    }
}

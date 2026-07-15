package com.marcosmoreiradev.docupodcaststudio.presentation.theatre;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TheatreFrameSketchDialogSourceTest {
    @Test
    void frameEditorUsesFixedLogicalCanvasAndMaximizableWindow() throws Exception {
        String dialog = Files.readString(Path.of(
                "src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/theatre/TheatreFrameSketchDialog.java"));
        String surface = Files.readString(Path.of(
                "src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/ink/canvas/InkCanvasSurface.java"));

        assertTrue(dialog.contains("surface.resetForFixedEditableState(FRAME_WIDTH, FRAME_HEIGHT"));
        assertTrue(dialog.contains("InkCanvasViewportCoordinateMapper.mapInside"));
        assertTrue(dialog.contains("Mostrar fragmento como titulo en el lienzo"));
        assertTrue(dialog.contains("FrameTitleState"));
        assertTrue(dialog.contains("METADATA_SHOW_FRAGMENT_TITLE"));
        assertTrue(dialog.contains("InkWorkspaceStateSerializer.fromJson"));
        assertTrue(dialog.contains("drawFrameTitle("));
        assertTrue(dialog.contains("theatre-frame-fragment-preview"));
        assertFalse(dialog.contains("TextArea text = new TextArea"));
        assertFalse(dialog.contains("Label title = new Label(context.title()"));
        assertFalse(dialog.contains("new VBox(12, title, fragmentPreview"));
        assertTrue(dialog.contains("stage.setMaximized(true)"));
        assertTrue(dialog.contains("new Image(uri, FRAME_WIDTH, FRAME_HEIGHT, false, true, false)"));
        assertTrue(surface.contains("setFixedLogicalViewport"));
        assertTrue(surface.contains("inkInputTarget().setWidth(fixedLogicalWidth)"));
        assertTrue(surface.contains("setClip(new Rectangle(fixedLogicalWidth, fixedLogicalHeight))"));
    }

    @Test
    void frameEditorShowsOnlySceneObjectImagesAndLinksHistoryBoard() throws Exception {
        String dialog = Files.readString(Path.of(
                "src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/theatre/TheatreFrameSketchDialog.java"));
        String workflow = Files.readString(Path.of(
                "src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/TheatreStoryboardFrameWorkflow.java"));
        String context = Files.readString(Path.of(
                "src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/theatre/TheatreFrameSketchContext.java"));
        String css = Files.readString(Path.of("src/main/resources/css/document/study-problem.css"));

        assertTrue(dialog.contains("Ver history board"));
        assertTrue(dialog.contains("Utilizar plano asignado"));
        assertTrue(dialog.contains("Transparencia del plano"));
        assertTrue(dialog.contains("useCameraGuide.selectedProperty()"));
        assertTrue(dialog.contains("canvasFrame.getChildren().add(surface)"));
        assertTrue(dialog.contains("guide.opacityProperty().bind(cameraGuideOpacity.valueProperty().divide(100.0))"));
        assertTrue(dialog.contains("setResultConverter(button -> button == saveButtonType ? saveResult() : null)"));
        assertTrue(dialog.contains("lookupButton(saveButtonType)"));
        assertTrue(dialog.contains("showHistoryBoard"));
        assertTrue(dialog.contains("Objetos de la escena"));
        assertTrue(dialog.contains("Sin imagenes de objetos en esta escena."));
        assertFalse(dialog.contains("associatedObjects"));
        assertFalse(context.contains("associatedObjects"));

        assertTrue(workflow.contains("sceneObjectPreviews"));
        assertTrue(workflow.contains("theatre.objectImages().stream()"));
        assertTrue(workflow.contains(".filter(image -> image.sceneId().equals(sceneId))"));
        assertTrue(workflow.contains("if (imageUri.isBlank()) continue;"));
        assertTrue(dialog.contains("cameraGuideLayer"));
        assertTrue(dialog.contains("context.cameraGuideUri()"));
        assertFalse(dialog.contains("surface.opacityProperty().bind"));
        assertTrue(css.contains(".theatre-camera-guide-veil"));
        assertTrue(workflow.contains("TheatreCameraReferenceResolver"));
        assertTrue(workflow.contains("TheatreCameraApplicationPolicy"));
        assertTrue(context.contains("cameraGuideUri"));
        assertFalse(workflow.contains("associatedObjectLabels"));
    }
}

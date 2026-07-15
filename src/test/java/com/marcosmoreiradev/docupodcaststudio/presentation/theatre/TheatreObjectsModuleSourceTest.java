package com.marcosmoreiradev.docupodcaststudio.presentation.theatre;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class TheatreObjectsModuleSourceTest {
    @Test
    void theatreObjectsModuleUsesSplitPanelEditorAndProjectPersistence() throws Exception {
        String dock = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/theatre/TheatreSideDock.java");
        String panel = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/theatre/TheatreObjectsPanel.java");
        String viewModel = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java");
        String coordinator = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/TheatreObjectProfileCoordinator.java");
        String imageCoordinator = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/TheatreObjectImageCoordinator.java");

        assertTrue(dock.contains("() -> objects(viewModel)"));
        assertTrue(dock.contains("new TheatreObjectsPanel(viewModel)"));
        assertTrue(panel.contains("new CollapsibleModuleSplitPane(\"Ficha\", inspector, \"Fichas de objetos\", rail, 0.45)"));
        assertTrue(panel.contains("ListView<TheatreObjectPresentation>"));
        assertTrue(panel.contains("Nuevo objeto"));
        assertTrue(panel.contains("Nuevo acto"));
        assertTrue(panel.contains("Agregar escena"));
        assertTrue(panel.contains("TheatreWorkspaceEmptyState.noActsMessage"));
        assertTrue(panel.contains("TheatreWorkspaceEmptyState.noScenesMessage"));
        assertTrue(panel.contains("selectedScene"));
        assertTrue(panel.contains("secondaryVisibleProperty()"));
        assertTrue(panel.contains("Ocultar fichas de objetos"));
        assertTrue(panel.contains("TechnicalSheetTextArea description"));
        assertTrue(panel.contains("Ejemplo de ficha tecnica"));
        assertTrue(panel.contains("TextField name"));
        assertTrue(panel.contains("Ver ficha"));
        assertTrue(panel.contains("chooseObjectSceneImage"));
        assertTrue(panel.contains("theatreObjectSceneImages"));
        assertTrue(panel.contains("AppIcon.FULLSCREEN"));
        assertTrue(panel.contains("ImageFullscreenViewer.show"));
        assertTrue(panel.contains("firstSceneObjectImageUri"));
        assertTrue(panel.contains("objectSceneDescription"));
        assertTrue(panel.contains("return object.cardPreview()"));
        assertTrue(viewModel.contains("theatreObjects()"));
        assertTrue(viewModel.contains("saveTheatreObjectDescription"));
        assertTrue(viewModel.contains("addTheatreObjectSceneImage"));
        assertTrue(viewModel.contains("replaceTheatreObjectSceneImage"));
        assertTrue(viewModel.contains("updateTheatreObjectSceneImageNote"));
        assertTrue(viewModel.contains("deleteTheatreObjectSceneImage"));
        assertTrue(coordinator.contains("resolve(\"objetos\")"));
        assertTrue(coordinator.contains("resolve(\"ficha-tecnica.txt\")"));
        assertTrue(coordinator.contains("resolve(\"imagenes\")"));
        assertTrue(imageCoordinator.contains("record SaveResult"));
        assertTrue(imageCoordinator.contains("replaceObjectImages"));
    }

    @Test
    void theatreProjectLayerOwnsObjectsInJsonContract() throws Exception {
        String layer = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/domain/theatre/TheatreProjectLayer.java");
        String writer = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/json/DocuPodcastProjectJsonWriter.java");
        String reader = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/json/DocuPodcastProjectJsonReader.java");

        assertTrue(layer.contains("List<TheatreObject> objects"));
        assertTrue(layer.contains("List<ObjectImage> objectImages"));
        assertTrue(layer.contains("record TheatreObject"));
        assertTrue(layer.contains("record ObjectImage"));
        assertTrue(writer.contains("writeObjectImages(out, theatre.objectImages(), 2)"));
        assertTrue(writer.contains("writeTheatreObjects(out, theatre.objects(), 2)"));
        assertTrue(reader.contains("readObjectImages(theatre.get(\"objectImages\"))"));
        assertTrue(reader.contains("readTheatreObjects(theatre.get(\"objects\"))"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}

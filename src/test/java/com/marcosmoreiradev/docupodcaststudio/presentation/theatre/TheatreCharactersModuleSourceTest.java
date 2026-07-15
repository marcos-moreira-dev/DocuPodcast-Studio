package com.marcosmoreiradev.docupodcaststudio.presentation.theatre;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TheatreCharactersModuleSourceTest {
    @Test
    void theatreCharactersModuleUsesSplitPanelAndDescriptionEditor() throws Exception {
        String dock = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/theatre/TheatreSideDock.java");
        String panel = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/theatre/TheatreCharactersPanel.java");
        String viewModel = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java");
        String coordinator = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/TheatreCharacterProfileCoordinator.java");

        assertTrue(dock.contains("() -> characters(viewModel)"));
        assertTrue(dock.contains("new TheatreCharactersPanel(viewModel)"));
        assertTrue(panel.contains("new CollapsibleModuleSplitPane(\"Ficha\", inspector, \"Fichas de personajes\", rail, 0.45)"));
        assertTrue(panel.contains("ListView<TheatreCharacterPresentation>"));
        assertTrue(panel.contains("secondaryVisibleProperty()"));
        assertTrue(panel.contains("Ocultar fichas de personajes"));
        assertTrue(panel.contains("TechnicalSheetTextArea description"));
        assertTrue(panel.contains("Ejemplo de ficha t\\u00e9cnica"));
        assertTrue(panel.contains("root.requestFocus()"));
        assertFalse(panel.contains("character.mentionLabel()"));
        assertFalse(panel.contains("description.requestFocus()"));
        assertFalse(panel.contains("Sin imagen asociada"));
        assertFalse(panel.contains("Este lado queda reservado para fotos"));
        assertTrue(panel.contains("Ver descripci\\u00f3n"));
        assertTrue(panel.contains("Asignar voz"));
        assertTrue(panel.contains("showVoiceAssignmentDialog"));
        assertTrue(panel.contains("ComboBox<VoiceAssignmentOption>"));
        assertTrue(panel.contains("availableVoiceChoices"));
        assertTrue(panel.contains("buildVoiceAssignmentOptions()"));
        assertTrue(panel.contains("Las voces usadas por otros personajes o no disponibles con el motor activo aparecen bloqueadas."));
        assertTrue(panel.contains("!selected.selectable()"));
        assertFalse(panel.contains("VoiceProfile::usableForTts"));
        assertTrue(panel.contains("usedByOthers"));
        assertTrue(panel.contains("viewModel.assignTheatreCharacterVoice(character.id(), character.displayName(), selected.voiceId())"));
        assertTrue(panel.contains("emptyImageView()"));
        assertTrue(panel.contains("characterAvatarBox(character)"));
        assertTrue(panel.contains("firstSceneCharacterImageUri"));
        assertTrue(panel.contains("viewModel.theatreScenes().stream().findFirst()"));
        assertTrue(panel.contains("viewModel.theatreCharacterSceneImages(character.id(), scene.id()).stream().findFirst()"));
        assertTrue(read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/theatre/TechnicalSheetTextArea.java")
                .contains("textArea.getText().isBlank() && !textArea.isFocused()"));
        assertTrue(viewModel.contains("saveTheatreCharacterDescription"));
        assertTrue(viewModel.contains("theatreVoiceRoleAliases()"));
        assertTrue(viewModel.contains("theatreVoiceRoleAliasForCharacter"));
        assertTrue(viewModel.contains("assignTheatreCharacterVoice"));
        assertTrue(viewModel.contains("VoiceRoleAlias") || coordinator.contains("VoiceRoleAlias"));
        assertTrue(coordinator.contains("replaceVoiceRoleAliases") || viewModel.contains("replaceTheatreVoiceRoleAliases"));
        assertTrue(coordinator.contains("withTheatre(replaceCharacters(theatre, updated))")
                || coordinator.contains("replaceVoiceRoleAliases"));
    }

    @Test
    void theatreCharactersLeftPaneManagesCollapsibleActsAndScenes() throws Exception {
        String panel = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/theatre/TheatreCharactersPanel.java");
        String viewModel = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java");
        String coordinator = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/TheatreSceneCoordinator.java");

        assertTrue(panel.contains("Nuevo acto"));
        assertTrue(panel.contains("Vestuario de personaje por escena"));
        assertFalse(panel.contains("\"Actos\","));
        assertTrue(panel.contains("showActEditor"));
        assertTrue(panel.contains("titleEditButton"));
        assertTrue(panel.contains("Editar nombre del acto"));
        assertTrue(panel.contains("Editar nombre de la escena"));
        assertTrue(panel.contains("MouseEvent.MOUSE_CLICKED"));
        assertTrue(panel.contains("TheatreProjectLayer.TheatreAct"));
        assertTrue(panel.contains("expandedActIds"));
        assertTrue(panel.contains("expandedSceneIds"));
        assertTrue(panel.contains("TheatreWorkspaceEmptyState.noActsMessage"));
        assertTrue(panel.contains("TheatreWorkspaceEmptyState.noScenesMessage"));
        assertTrue(panel.contains("Agregar escena"));
        assertTrue(panel.contains("Editar escena"));
        assertTrue(panel.contains("Eliminar escena"));
        assertTrue(panel.contains("selectedScene"));
        assertTrue(panel.contains("showSceneEditor"));
        assertTrue(panel.contains("viewModel.addTheatreAct"));
        assertTrue(panel.contains("viewModel.updateTheatreAct"));
        assertTrue(panel.contains("viewModel.addTheatreScene(act.id()"));
        assertTrue(panel.contains("viewModel.updateTheatreScene"));
        assertTrue(panel.contains("viewModel.deleteTheatreScene"));
        assertTrue(panel.contains("imageFullscreenButton"));
        assertTrue(panel.contains("AppIcon.FULLSCREEN"));
        assertTrue(panel.contains("Ver foto en pantalla completa"));
        assertTrue(panel.contains("ImageFullscreenViewer.show"));
        assertTrue(panel.contains("refreshAfterCharacterImageChange()"));
        assertTrue(viewModel.contains("theatreActs()"));
        assertTrue(viewModel.contains("addTheatreAct"));
        assertTrue(viewModel.contains("updateTheatreAct"));
        assertTrue(viewModel.contains("theatreScenes()"));
        assertTrue(viewModel.contains("addTheatreScene"));
        assertTrue(viewModel.contains("updateTheatreScene"));
        assertTrue(viewModel.contains("deleteTheatreScene"));
        assertTrue(coordinator.contains("addAct"));
        assertTrue(coordinator.contains("updateAct"));
        assertTrue(coordinator.contains("addScene"));
        assertTrue(coordinator.contains("updateScene"));
        assertTrue(coordinator.contains("deleteScene"));
        assertTrue(coordinator.contains("displayActs"));
        assertTrue(coordinator.contains("actId"));
    }

    @Test
    void theatreCharacterDetectorKeepsTheFirstColonRuleVisibleInSource() throws Exception {
        String detector = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/theatre/TheatreCharacterDetector.java");

        assertTrue(detector.contains("int firstColon = line.indexOf(':')"));
        assertTrue(detector.contains("line.substring(firstColon + 1)"));
        assertTrue(detector.contains("upper.matches(\"INTERVENCION-\\\\d+\")"));
        assertTrue(detector.contains("upper.matches(\"ESCENA\\\\s+\\\\d+.*\")"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}

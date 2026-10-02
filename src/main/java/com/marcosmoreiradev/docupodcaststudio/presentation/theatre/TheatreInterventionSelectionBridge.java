package com.marcosmoreiradev.docupodcaststudio.presentation.theatre;

import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.DocuPodcastShellViewModel;
import javafx.beans.property.StringProperty;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/** Keeps theatre intervention selection coherent across document, maps and AI navigator. */
final class TheatreInterventionSelectionBridge {
    private TheatreInterventionSelectionBridge() {
    }

    static void bind(
            DocuPodcastShellViewModel viewModel,
            StringProperty selectedIntervencion,
            IntervencionBoundaryStore boundaryStore) {
        if (viewModel == null || selectedIntervencion == null) {
            return;
        }
        Runnable sync = () -> syncFromDocumentSelection(viewModel, selectedIntervencion, boundaryStore);
        viewModel.selectedDocumentBlockIdProperty().addListener((obs, oldValue, newValue) -> sync.run());
        viewModel.focusedTheatreSceneIdProperty().addListener((obs, oldValue, newValue) -> sync.run());
        if (boundaryStore != null) {
            boundaryStore.revisionProperty().addListener((obs, oldValue, newValue) -> sync.run());
        }
        sync.run();
    }

    static Consumer<String> blockSelector(DocuPodcastShellViewModel viewModel, TheatreProjectLayer.Scene scene) {
        return blockId -> selectBlock(viewModel, scene, blockId);
    }

    static void selectBlock(DocuPodcastShellViewModel viewModel, TheatreProjectLayer.Scene scene, String blockId) {
        if (viewModel == null) {
            return;
        }
        String normalized = blockId == null ? "" : blockId.strip();
        if (!normalized.isBlank() && scene != null && scene.id() != null && !scene.id().isBlank()) {
            viewModel.focusTheatreScene(scene.id());
        }
        viewModel.selectDocumentBlock(normalized);
    }

    static String aliasForBlock(
            String blockId,
            String focusedSceneId,
            List<IntervencionCatalogo.IntervencionInfo> globalAliases,
            List<TheatreProjectLayer.Scene> scenes,
            IntervencionBoundaryStore boundaryStore) {
        String normalizedBlock = blockId == null ? "" : blockId.strip();
        if (normalizedBlock.isBlank() || globalAliases == null || globalAliases.isEmpty()) {
            return "";
        }
        Optional<TheatreProjectLayer.Scene> focused = focusedScene(focusedSceneId, scenes);
        if (focused.isPresent()) {
            String focusedAlias = IntervencionNumberingScene.intervencionesParaEscena(
                            globalAliases,
                            scenes == null ? List.of() : scenes,
                            boundaryStore,
                            focused.get())
                    .stream()
                    .filter(alias -> normalizedBlock.equals(alias.blockId()))
                    .map(IntervencionCatalogo.IntervencionInfo::alias)
                    .findFirst()
                    .orElse("");
            if (!focusedAlias.isBlank()) {
                return focusedAlias;
            }
        }
        // A document click may cross a scene boundary before the scene fold receives
        // focus. Fall back to the canonical global catalog so the first stage
        // direction of the next scene is still selected instead of being discarded.
        return globalAliases.stream()
                .filter(alias -> normalizedBlock.equals(alias.blockId()))
                .map(IntervencionCatalogo.IntervencionInfo::alias)
                .findFirst()
                .orElse("");
    }

    private static void syncFromDocumentSelection(
            DocuPodcastShellViewModel viewModel,
            StringProperty selectedIntervencion,
            IntervencionBoundaryStore boundaryStore) {
        String blockId = viewModel.selectedDocumentBlockIdProperty().get();
        List<IntervencionCatalogo.IntervencionInfo> aliases = IntervencionCatalogo.intervenciones(
                viewModel.currentDocumentProperty().get(), viewModel.currentScriptProperty().get());
        sceneForBlock(blockId, aliases, viewModel.theatreScenes(), boundaryStore)
                .map(TheatreProjectLayer.Scene::id)
                .filter(sceneId -> !sceneId.equals(viewModel.focusedTheatreSceneIdProperty().get()))
                .ifPresent(viewModel::focusTheatreScene);
        String alias = aliasForBlock(
                blockId,
                viewModel.focusedTheatreSceneIdProperty().get(),
                aliases,
                viewModel.theatreScenes(),
                boundaryStore);
        if (!alias.equals(selectedIntervencion.get())) {
            selectedIntervencion.set(alias);
        }
    }

    static Optional<TheatreProjectLayer.Scene> sceneForBlock(
            String blockId,
            List<IntervencionCatalogo.IntervencionInfo> globalAliases,
            List<TheatreProjectLayer.Scene> scenes,
            IntervencionBoundaryStore boundaryStore) {
        String normalized = blockId == null ? "" : blockId.strip();
        if (normalized.isBlank() || scenes == null || boundaryStore == null) {
            return Optional.empty();
        }
        return scenes.stream()
                .filter(scene -> IntervencionNumberingScene.intervencionesParaEscena(
                                globalAliases, scenes, boundaryStore, scene)
                        .stream()
                        .anyMatch(alias -> normalized.equals(alias.blockId())))
                .findFirst();
    }

    private static Optional<TheatreProjectLayer.Scene> focusedScene(
            String focusedSceneId,
            List<TheatreProjectLayer.Scene> scenes) {
        String normalized = focusedSceneId == null ? "" : focusedSceneId.strip();
        if (normalized.isBlank() || scenes == null || scenes.isEmpty()) {
            return Optional.empty();
        }
        return scenes.stream()
                .filter(scene -> scene != null && normalized.equals(scene.id()))
                .findFirst();
    }
}

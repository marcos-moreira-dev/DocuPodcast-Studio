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
            return IntervencionNumberingScene.intervencionesParaEscena(
                            globalAliases,
                            scenes == null ? List.of() : scenes,
                            boundaryStore,
                            focused.get())
                    .stream()
                    .filter(alias -> normalizedBlock.equals(alias.blockId()))
                    .map(IntervencionCatalogo.IntervencionInfo::alias)
                    .findFirst()
                    .orElse("");
        }
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
        String alias = aliasForBlock(
                viewModel.selectedDocumentBlockIdProperty().get(),
                viewModel.focusedTheatreSceneIdProperty().get(),
                IntervencionCatalogo.intervenciones(viewModel.currentDocumentProperty().get(), viewModel.currentScriptProperty().get()),
                viewModel.theatreScenes(),
                boundaryStore);
        if (!alias.equals(selectedIntervencion.get())) {
            selectedIntervencion.set(alias);
        }
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

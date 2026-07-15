package com.marcosmoreiradev.docupodcaststudio.presentation.theatre;

import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.ActionButtonFactory;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.DocuPodcastShellViewModel;
import javafx.beans.property.StringProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** Read-only act/scene/intervention navigator for theatre AI workflows. */
final class TheatreInterventionNavigator extends VBox {
    private final DocuPodcastShellViewModel viewModel;
    private final StringProperty selectedIntervention;
    private final Consumer<TheatreProjectLayer.TheatreAct> generateAct;
    private final Consumer<TheatreProjectLayer.Scene> generateScene;
    private final BiConsumer<TheatreProjectLayer.Scene, IntervencionCatalogo.IntervencionInfo> generateIntervention;
    private final BiConsumer<TheatreProjectLayer.Scene, IntervencionCatalogo.IntervencionInfo> contextExport;
    private final Set<String> expandedActIds = new LinkedHashSet<>();
    private final Set<String> expandedSceneIds = new LinkedHashSet<>();
    private boolean expansionInitialized;

    TheatreInterventionNavigator(
            DocuPodcastShellViewModel viewModel,
            StringProperty selectedIntervention,
            BiConsumer<TheatreProjectLayer.Scene, IntervencionCatalogo.IntervencionInfo> contextExport) {
        this(viewModel, selectedIntervention, null, null, null, contextExport);
    }

    TheatreInterventionNavigator(
            DocuPodcastShellViewModel viewModel,
            StringProperty selectedIntervention,
            Consumer<TheatreProjectLayer.TheatreAct> generateAct,
            Consumer<TheatreProjectLayer.Scene> generateScene,
            BiConsumer<TheatreProjectLayer.Scene, IntervencionCatalogo.IntervencionInfo> generateIntervention,
            BiConsumer<TheatreProjectLayer.Scene, IntervencionCatalogo.IntervencionInfo> contextExport) {
        this.viewModel = viewModel;
        this.selectedIntervention = selectedIntervention;
        this.generateAct = generateAct == null ? ignored -> { } : generateAct;
        this.generateScene = generateScene == null ? ignored -> { } : generateScene;
        this.generateIntervention = generateIntervention == null ? (scene, alias) -> { } : generateIntervention;
        this.contextExport = contextExport == null ? (scene, alias) -> { } : contextExport;
        getStyleClass().addAll("theatre-act-list", "theatre-shared-scene-folds", "theatre-ai-intervention-navigator");
        setSpacing(8);
        refresh();
        viewModel.currentDocumentProperty().addListener((obs, oldValue, newValue) -> refresh());
        viewModel.currentScriptProperty().addListener((obs, oldValue, newValue) -> refresh());
        viewModel.dirtyProperty().addListener((obs, oldValue, newValue) -> refresh());
        TheatreInterventionSelectionBridge.bind(viewModel, selectedIntervention, viewModel.intervencionBoundaryStore());
    }

    void refresh() {
        List<TheatreProjectLayer.TheatreAct> acts = viewModel.theatreActs();
        List<TheatreProjectLayer.Scene> scenes = viewModel.theatreScenes();
        ReadableDocument document = viewModel.currentDocumentProperty().get();
        NarrationScriptDocument script = viewModel.currentScriptProperty().get();
        if (!expansionInitialized) {
            acts.forEach(act -> expandedActIds.add(act.id()));
            scenes.stream().findFirst().ifPresent(scene -> expandedSceneIds.add(scene.id()));
            expansionInitialized = true;
        }
        getChildren().clear();
        if (acts.isEmpty()) {
            getChildren().add(emptyNote(TheatreWorkspaceEmptyState.noActsMessage(document, script)));
            return;
        }
        acts.forEach(act -> getChildren().add(actCard(act, scenesForAct(act, scenes))));
    }

    private Node actCard(TheatreProjectLayer.TheatreAct act, List<TheatreProjectLayer.Scene> scenes) {
        boolean expanded = expandedActIds.contains(act.id());
        VBox card = new VBox(0);
        card.getStyleClass().add("theatre-act-card");
        card.setMaxWidth(Double.MAX_VALUE);

        Label chevron = new Label(expanded ? "\u25be" : "\u25b8");
        chevron.getStyleClass().add("theatre-act-chevron");
        Label name = new Label(act.displayName());
        name.getStyleClass().add("theatre-act-name");
        name.setWrapText(true);
        Label count = new Label(sceneCountLabel(scenes.size()));
        count.getStyleClass().add("theatre-act-summary");
        Button process = headerButton("Procesar acto", () -> generateAct.accept(act));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(7, chevron, name, spacer, process, count);
        header.getStyleClass().add("theatre-act-header");
        header.setAlignment(Pos.CENTER_LEFT);
        header.setOnMouseClicked(event -> {
            toggle(expandedActIds, act.id());
            refresh();
            event.consume();
        });
        card.getChildren().add(header);

        if (expanded) {
            VBox body = new VBox(8);
            body.getStyleClass().add("theatre-act-body");
            if (scenes.isEmpty()) {
                body.getChildren().add(emptyNote(TheatreWorkspaceEmptyState.noScenesMessage(act)));
            } else {
                scenes.forEach(scene -> body.getChildren().add(sceneCard(scene)));
            }
            card.getChildren().add(body);
        }
        return card;
    }

    private Node sceneCard(TheatreProjectLayer.Scene scene) {
        boolean expanded = expandedSceneIds.contains(scene.id());
        VBox card = new VBox(0);
        card.getStyleClass().add("theatre-scene-card");
        card.setMinWidth(0);
        card.setMaxWidth(Double.MAX_VALUE);

        Label chevron = new Label(expanded ? "\u25be" : "\u25b8");
        chevron.getStyleClass().add("theatre-act-chevron");
        Label title = new Label(scene.displayName());
        title.getStyleClass().add("theatre-scene-name");
        title.setWrapText(true);
        Button process = headerButton("Procesar escena", () -> generateScene.accept(scene));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(7, chevron, title, spacer, process);
        header.getStyleClass().add("theatre-scene-header");
        header.setAlignment(Pos.CENTER_LEFT);
        header.setOnMouseClicked(event -> {
            viewModel.focusTheatreScene(scene.id());
            toggle(expandedSceneIds, scene.id());
            refresh();
            event.consume();
        });
        card.getChildren().add(header);

        if (expanded) {
            VBox body = new VBox(7);
            body.getStyleClass().add("theatre-scene-body");
            Label description = new Label(scene.notes().isBlank()
                    ? "Sin descripcion."
                    : TheatreCharacterPresentation.excerpt(scene.notes(), 180));
            description.getStyleClass().add("theatre-scene-description");
            description.setWrapText(true);
            body.getChildren().addAll(description, sceneCanvas(scene));
            card.getChildren().add(body);
        }
        return card;
    }

    private Node sceneCanvas(TheatreProjectLayer.Scene scene) {
        ReadableDocument document = viewModel.currentDocumentProperty().get();
        NarrationScriptDocument script = viewModel.currentScriptProperty().get();
        List<IntervencionCatalogo.IntervencionInfo> aliases = intervencionesParaEscena(scene);
        TheatreTextSequenceCanvas canvas = new TheatreTextSequenceCanvas(
                scene.displayName(),
                TheatreWorkspaceEmptyState.interventionSequenceMessage(document, script),
                aliases,
                selectedIntervention,
                TheatreInterventionSelectionBridge.blockSelector(viewModel, scene),
                null,
                alias -> generateIntervention.accept(scene, alias),
                alias -> contextExport.accept(scene, alias));
        ScrollPane scroll = new ScrollPane(canvas);
        scroll.getStyleClass().add("theatre-action-canvas-scroll");
        scroll.setFitToWidth(false);
        scroll.setFitToHeight(false);
        scroll.setPrefViewportHeight(240);
        scroll.setMinViewportHeight(210);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        VBox box = new VBox(5);
        TheatreWorkspaceEmptyState.preparationNotice(document, script)
                .map(TheatreInterventionNavigator::emptyNote)
                .ifPresent(box.getChildren()::add);
        box.getChildren().add(scroll);
        return box;
    }

    private List<IntervencionCatalogo.IntervencionInfo> intervencionesParaEscena(TheatreProjectLayer.Scene scene) {
        List<IntervencionCatalogo.IntervencionInfo> global = IntervencionCatalogo.intervenciones(
                viewModel.currentDocumentProperty().get(),
                viewModel.currentScriptProperty().get());
        return IntervencionNumberingScene.intervencionesParaEscena(
                global,
                viewModel.theatreScenes(),
                viewModel.intervencionBoundaryStore(),
                scene);
    }

    private List<TheatreProjectLayer.Scene> scenesForAct(TheatreProjectLayer.TheatreAct act, List<TheatreProjectLayer.Scene> scenes) {
        String actId = act == null ? "" : act.id();
        return scenes.stream().filter(scene -> scene.actId().equals(actId)).toList();
    }

    private static void toggle(Set<String> set, String id) {
        if (id == null || id.isBlank()) {
            return;
        }
        if (!set.add(id)) {
            set.remove(id);
        }
    }

    private static Label emptyNote(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("theatre-scene-empty-note");
        label.setWrapText(true);
        return label;
    }

    private static String sceneCountLabel(int count) {
        return count == 1 ? "1 escena" : count + " escenas";
    }

    private static Button headerButton(String text, Runnable action) {
        Button button = ActionButtonFactory.secondary(text, action);
        button.getStyleClass().add("theatre-ai-header-action");
        button.setMinWidth(128);
        button.setPrefWidth(142);
        button.setMaxWidth(160);
        button.setOnMouseClicked(event -> event.consume());
        return button;
    }
}

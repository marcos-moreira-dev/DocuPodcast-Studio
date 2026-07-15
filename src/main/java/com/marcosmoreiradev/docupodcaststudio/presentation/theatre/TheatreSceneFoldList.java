package com.marcosmoreiradev.docupodcaststudio.presentation.theatre;

import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.ActionButtonFactory;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.DocuPodcastShellViewModel;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/** Shared act/scene folds for theatre modules that do not own images directly. */
final class TheatreSceneFoldList extends VBox {
    private static final String DEFAULT_ACT_ID = "ACT-ACTO-1";

    private final DocuPodcastShellViewModel viewModel;
    private final Function<TheatreProjectLayer.Scene, Node> sceneContentFactory;
    private final Set<String> expandedActIds = new LinkedHashSet<>();
    private final Set<String> expandedSceneIds = new LinkedHashSet<>();
    private boolean expansionInitialized;

    TheatreSceneFoldList(
            DocuPodcastShellViewModel viewModel,
            Function<TheatreProjectLayer.Scene, Node> sceneContentFactory) {
        this.viewModel = viewModel;
        this.sceneContentFactory = sceneContentFactory == null ? scene -> null : sceneContentFactory;
        getStyleClass().addAll("theatre-act-list", "theatre-shared-scene-folds");
        setSpacing(8);
        refresh();
        viewModel.currentDocumentProperty().addListener((obs, oldValue, newValue) -> refresh());
        viewModel.currentScriptProperty().addListener((obs, oldValue, newValue) -> refresh());
        viewModel.dirtyProperty().addListener((obs, oldValue, newValue) -> refresh());
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
        Button createAct = ActionButtonFactory.primary("Nuevo acto", () -> showActEditor(null));
        createAct.getStyleClass().add("theatre-scene-fold-create-button");
        getChildren().add(createAct);
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
        Button edit = titleEditButton("Editar acto", () -> showActEditor(act));
        Label count = new Label(sceneCountLabel(scenes.size()));
        count.getStyleClass().add("theatre-act-summary");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(7, chevron, name, edit, spacer, count);
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
            Button createScene = ActionButtonFactory.rail("Agregar escena", () -> showSceneEditor(act, null));
            createScene.setMaxWidth(Double.MAX_VALUE);
            body.getChildren().add(createScene);
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
        Button edit = titleEditButton("Editar escena", () -> showSceneEditor(sceneAct(scene), scene));
        Button delete = titleDeleteButton("Eliminar escena", () -> {
            viewModel.deleteTheatreScene(scene.id());
            expandedSceneIds.remove(scene.id());
            refresh();
        });
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(7, chevron, title, spacer, edit, delete);
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
            body.getChildren().add(description);
            Node content = sceneContentFactory.apply(scene);
            if (content != null) {
                body.getChildren().add(content);
            }
            card.getChildren().add(body);
        }
        return card;
    }

    private TheatreProjectLayer.TheatreAct sceneAct(TheatreProjectLayer.Scene scene) {
        String actId = sceneActId(scene);
        return viewModel.theatreActs().stream()
                .filter(act -> act.id().equals(actId))
                .findFirst()
                .orElseGet(() -> viewModel.theatreActs().stream().findFirst().orElse(null));
    }

    private Button titleEditButton(String tooltip, Runnable action) {
        Button button = ActionButtonFactory.secondary("\u270e", tooltip, action);
        button.getStyleClass().add("theatre-title-edit-button");
        sizeTitleButton(button);
        button.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> event.consume());
        return button;
    }

    private Button titleDeleteButton(String tooltip, Runnable action) {
        Button button = ActionButtonFactory.danger("X", tooltip, action);
        button.getStyleClass().addAll("theatre-title-edit-button", "theatre-title-delete-button");
        sizeTitleButton(button);
        button.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> event.consume());
        return button;
    }

    private static void sizeTitleButton(Button button) {
        button.setMinSize(24, 24);
        button.setPrefSize(24, 24);
        button.setMaxSize(24, 24);
    }

    private void showActEditor(TheatreProjectLayer.TheatreAct act) {
        boolean creating = act == null;
        Label title = dialogTitle(creating ? "Nuevo acto" : "Editar acto");
        TextField name = new TextField(creating ? "Acto " + (viewModel.theatreActs().size() + 1) : act.displayName());
        name.getStyleClass().add("theatre-object-name-field");
        name.setPromptText("Nombre del acto");
        name.setMaxWidth(Double.MAX_VALUE);
        TechnicalSheetTextArea description = new TechnicalSheetTextArea(
                creating ? "" : act.notes(),
                "Descripcion del acto, continuidad y objetivo dramatico.");
        Button accept = ActionButtonFactory.primary("Aceptar", () -> {
            if (creating) {
                viewModel.addTheatreAct(name.getText(), description.text());
                expansionInitialized = false;
            } else {
                viewModel.updateTheatreAct(act.id(), name.getText(), description.text());
                expandedActIds.add(act.id());
            }
            refresh();
            ((Stage) description.getScene().getWindow()).close();
        });
        Button close = ActionButtonFactory.secondary("Cerrar", () ->
                ((Stage) description.getScene().getWindow()).close());
        showNamedDescriptionDialog(creating ? "Nuevo acto" : "Editar acto", title, name, description, accept, close);
    }

    private void showSceneEditor(TheatreProjectLayer.TheatreAct act, TheatreProjectLayer.Scene scene) {
        if (act == null && scene == null) {
            return;
        }
        boolean creating = scene == null;
        Label title = dialogTitle(creating ? "Nueva escena en " + act.displayName() : "Editar escena");
        TextField name = new TextField(creating ? "" : scene.displayName());
        name.getStyleClass().add("theatre-object-name-field");
        name.setPromptText("Nombre de la escena");
        name.setMaxWidth(Double.MAX_VALUE);
        TechnicalSheetTextArea description = new TechnicalSheetTextArea(
                creating ? "" : scene.notes(),
                "Descripcion de la escena, lugar, continuidad visual y proposito.");
        Button accept = ActionButtonFactory.primary("Aceptar", () -> {
            if (creating) {
                viewModel.addTheatreScene(act.id(), name.getText(), description.text());
                expandedActIds.add(act.id());
            } else {
                viewModel.updateTheatreScene(scene.id(), name.getText(), description.text());
                expandedSceneIds.add(scene.id());
            }
            refresh();
            ((Stage) description.getScene().getWindow()).close();
        });
        Button close = ActionButtonFactory.secondary("Cerrar", () ->
                ((Stage) description.getScene().getWindow()).close());
        showNamedDescriptionDialog(creating ? "Nueva escena" : "Editar escena", title, name, description, accept, close);
    }

    private Label dialogTitle(String text) {
        Label title = new Label(text);
        title.getStyleClass().add("theatre-character-dialog-title");
        title.setWrapText(true);
        return title;
    }

    private void showNamedDescriptionDialog(
            String windowTitle,
            Label title,
            TextField name,
            TechnicalSheetTextArea description,
            Button accept,
            Button close) {
        HBox actions = new HBox(8, accept, close);
        actions.getStyleClass().add("theatre-character-dialog-actions");
        actions.setAlignment(Pos.CENTER_RIGHT);
        VBox root = new VBox(12, title, name, description, actions);
        root.getStyleClass().add("theatre-character-dialog-root");
        root.setPadding(new Insets(14));
        root.setFocusTraversable(true);
        VBox.setVgrow(description, Priority.ALWAYS);

        Stage stage = new Stage();
        Window owner = getScene() == null ? null : getScene().getWindow();
        if (owner != null) {
            stage.initOwner(owner);
        }
        stage.setTitle(windowTitle);
        Scene sceneView = new Scene(root, 520, 420);
        if (getScene() != null) {
            sceneView.getStylesheets().addAll(getScene().getStylesheets());
        }
        stage.setScene(sceneView);
        stage.show();
        root.requestFocus();
    }

    private List<TheatreProjectLayer.Scene> scenesForAct(
            TheatreProjectLayer.TheatreAct act,
            List<TheatreProjectLayer.Scene> scenes) {
        return scenes.stream()
                .filter(scene -> sceneActId(scene).equals(act.id()))
                .toList();
    }

    private static String sceneActId(TheatreProjectLayer.Scene scene) {
        String actId = scene == null ? "" : scene.actId();
        return actId == null || actId.isBlank() ? DEFAULT_ACT_ID : actId;
    }

    private static String sceneCountLabel(int count) {
        return count == 1 ? "1 escena" : count + " escenas";
    }

    private static void toggle(Set<String> ids, String id) {
        if (id == null || id.isBlank()) {
            return;
        }
        if (!ids.remove(id)) {
            ids.add(id);
        }
    }

    private static Label emptyNote(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.getStyleClass().add("document-media-empty-note");
        return label;
    }
}

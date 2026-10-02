package com.marcosmoreiradev.docupodcaststudio.presentation.theatre;

import com.marcosmoreiradev.docupodcaststudio.presentation.dialogs.NativeSourceChooser;

import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioViewportControls;

import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioCollectionControls;

import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioFormControls;

import com.marcosmoreiradev.docupodcaststudio.presentation.components.ActionButtonFactory;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.ResponsiveActionGroup;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.AppIcon;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.CollapsibleModuleSplitPane;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.ImageFullscreenViewer;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.SectionHeader;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.SidePanelToggleButton;
import com.marcosmoreiradev.docupodcaststudio.application.voice.VoiceAssignmentOption;
import com.marcosmoreiradev.docupodcaststudio.application.voice.VoiceAssignmentReadinessContext;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.DocuPodcastShellViewModel;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Theatre module that lists detected characters and lets the user edit their profile notes. */
public final class TheatreCharactersPanel extends BorderPane {
    private static final String DEFAULT_ACT_ID = "ACT-ACTO-1";

    private final DocuPodcastShellViewModel viewModel;
    private final ObjectProperty<TheatreCharacterPresentation> selectedCharacter = new SimpleObjectProperty<>();
    private final ObjectProperty<TheatreProjectLayer.Scene> selectedScene = new SimpleObjectProperty<>();
    private final ListView<TheatreCharacterPresentation> characters = StudioCollectionControls.listView();
    private final VBox acts = new VBox(8);
    private final Set<String> expandedActIds = new LinkedHashSet<>();
    private final Set<String> expandedSceneIds = new LinkedHashSet<>();
    private final Label characterCount = new Label();
    private CollapsibleModuleSplitPane split;
    private boolean actExpansionInitialized;

    public TheatreCharactersPanel(DocuPodcastShellViewModel viewModel) {
        this.viewModel = viewModel;
        getStyleClass().add("theatre-characters-panel");

        VBox inspector = inspector();
        VBox rail = rail();
        split = new CollapsibleModuleSplitPane("Ficha", inspector, "Fichas de personajes", rail, 0.45);
        split.getStyleClass().add("theatre-characters-split");
        setCenter(split);

        refresh();
        viewModel.currentDocumentProperty().addListener((obs, oldValue, newValue) -> refresh());
        viewModel.currentScriptProperty().addListener((obs, oldValue, newValue) -> refresh());
        viewModel.dirtyProperty().addListener((obs, oldValue, newValue) -> refresh());
        selectedCharacter.addListener((obs, oldValue, newValue) -> refreshActs());
    }

    public BooleanProperty primaryVisibleProperty() {
        return split.primaryVisibleProperty();
    }

    public BooleanProperty secondaryVisibleProperty() {
        return split.secondaryVisibleProperty();
    }

    private VBox inspector() {
        VBox module = new VBox(10);
        module.getStyleClass().addAll("document-context-module", "theatre-character-inspector");
        module.setPadding(new Insets(10));
        module.setMaxHeight(Double.MAX_VALUE);
        module.setFillWidth(true);

        SectionHeader header = new SectionHeader(
                "Vestuario de personaje por escena",
                "Organiza referencias de vestuario y continuidad por escena.");

        Button create = ActionButtonFactory.primary("Nuevo acto", () -> showActEditor(null));
        create.getStyleClass().add("theatre-scene-create-button");

        acts.getStyleClass().add("theatre-act-list");
        acts.setMaxWidth(Double.MAX_VALUE);

        VBox body = new VBox(8, header, create, acts);
        body.getStyleClass().add("document-context-body");
        body.setMaxWidth(Double.MAX_VALUE);

        ScrollPane scroll = StudioViewportControls.scrollPane(body);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.getStyleClass().add("document-context-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        module.getChildren().add(scroll);
        return module;
    }

    private VBox rail() {
        VBox module = new VBox(10);
        module.getStyleClass().addAll("document-media-rail", "theatre-character-rail");
        module.setMaxHeight(Double.MAX_VALUE);
        module.setFillWidth(true);

        characterCount.getStyleClass().add("document-media-counter-label");
        characterCount.setWrapText(true);

        characters.getStyleClass().addAll("document-media-virtual-list", "theatre-character-list");
        characters.setPlaceholder(emptyNote("No hay personajes detectados. Usa lineas con formato PERSONAJE: texto."));
        characters.setCellFactory(list -> new CharacterCell());
        characters.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) ->
                selectedCharacter.set(newValue));
        VBox.setVgrow(characters, Priority.ALWAYS);

        VBox content = new VBox(10, characterActions(), railTitle("Fichas de personajes"), characterCount, characters);
        content.getStyleClass().addAll("document-media-visual-list", "theatre-character-list-shell");
        VBox.setVgrow(content, Priority.ALWAYS);
        module.getChildren().add(content);
        return module;
    }

    private HBox characterActions() {
        Label label = new Label("Acciones de personajes");
        label.getStyleClass().add("document-media-action-label");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button hide = new SidePanelToggleButton(
                AppIcon.COLLAPSE,
                "Ocultar fichas de personajes",
                () -> split.secondaryVisibleProperty().set(false));
        HBox row = new HBox(8, label, spacer, hide);
        row.getStyleClass().add("document-media-actions");
        return row;
    }

    private void refresh() {
        refreshActs();
        refreshCharacters();
    }

    private void refreshActs() {
        String selectedId = selectedScene.get() == null ? "" : selectedScene.get().id();
        List<TheatreProjectLayer.TheatreAct> nextActs = viewModel.theatreActs();
        List<TheatreProjectLayer.Scene> nextScenes = viewModel.theatreScenes();
        ReadableDocument document = viewModel.currentDocumentProperty().get();
        NarrationScriptDocument script = viewModel.currentScriptProperty().get();
        if (!actExpansionInitialized) {
            nextActs.forEach(act -> expandedActIds.add(act.id()));
            actExpansionInitialized = true;
        }

        acts.getChildren().clear();
        if (nextActs.isEmpty()) {
            acts.getChildren().add(emptyNote(TheatreWorkspaceEmptyState.noActsMessage(document, script)));
            selectedScene.set(null);
            return;
        }
        for (TheatreProjectLayer.TheatreAct act : nextActs) {
            acts.getChildren().add(actCard(act, scenesForAct(act, nextScenes)));
        }
        selectScene(selectedId, nextScenes);
    }

    private void selectScene(String selectedId, List<TheatreProjectLayer.Scene> availableScenes) {
        if (selectedId == null || selectedId.isBlank()) {
            return;
        }
        for (TheatreProjectLayer.Scene scene : availableScenes) {
            if (scene.id().equals(selectedId)) {
                selectedScene.set(scene);
                return;
            }
        }
        selectedScene.set(null);
    }

    private void refreshCharacters() {
        String selectedId = selectedCharacter.get() == null ? "" : selectedCharacter.get().id();
        List<TheatreCharacterPresentation> detected = TheatreCharacterDetector.detect(
                viewModel.currentDocumentProperty().get(),
                viewModel.currentScriptProperty().get(),
                viewModel.theatreCharacterProfiles());
        characters.getItems().setAll(detected);
        characterCount.setText(characterCountLabel(detected.size()));
        selectCharacter(selectedId);
    }

    private void selectCharacter(String selectedId) {
        if (selectedId == null || selectedId.isBlank()) {
            return;
        }
        for (TheatreCharacterPresentation character : characters.getItems()) {
            if (character.id().equals(selectedId)) {
                characters.getSelectionModel().select(character);
                selectedCharacter.set(character);
                return;
            }
        }
    }

    private String characterCountLabel(int count) {
        if (count <= 0) {
            return "Prepara la lectura o abre un guion con dialogos PERSONAJE: texto.";
        }
        return count + " personajes detectados. La lista muestra solo las fichas visibles para mantener fluido el documento.";
    }

    private List<TheatreProjectLayer.Scene> scenesForAct(
            TheatreProjectLayer.TheatreAct act,
            List<TheatreProjectLayer.Scene> availableScenes) {
        return availableScenes.stream()
                .filter(scene -> sceneActId(scene).equals(act.id()))
                .toList();
    }

    private Node actCard(TheatreProjectLayer.TheatreAct act, List<TheatreProjectLayer.Scene> actScenes) {
        boolean expanded = expandedActIds.contains(act.id());
        VBox card = new VBox(0);
        card.getStyleClass().add("theatre-act-card");
        card.setMaxWidth(Double.MAX_VALUE);

        Label chevron = new Label(expanded ? "\u25be" : "\u25b8");
        chevron.getStyleClass().add("theatre-act-chevron");

        Label name = new Label(act.displayName());
        name.getStyleClass().add("theatre-act-name");
        name.setWrapText(true);

        Button edit = titleEditButton("Editar nombre del acto", () -> showActEditor(act));
        Label count = new Label(sceneCountLabel(actScenes.size()));
        count.getStyleClass().add("theatre-act-summary");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(7, chevron, name, edit, spacer, count);
        header.getStyleClass().add("theatre-act-header");
        header.setAlignment(Pos.CENTER_LEFT);
        header.setOnMouseClicked(event -> {
            toggleExpanded(expandedActIds, act.id());
            refreshActs();
        });
        card.getChildren().add(header);

        if (expanded) {
            VBox body = new VBox(8);
            body.getStyleClass().add("theatre-act-body");
            if (!act.notes().isBlank()) {
                Label description = new Label(TheatreCharacterPresentation.excerpt(act.notes(), 120));
                description.getStyleClass().add("theatre-scene-description");
                description.setWrapText(true);
                body.getChildren().add(description);
            }
            body.getChildren().add(sceneActions(act));
            if (actScenes.isEmpty()) {
                body.getChildren().add(emptyNote(TheatreWorkspaceEmptyState.noScenesMessage(act)));
            } else {
                actScenes.forEach(scene -> body.getChildren().add(sceneCard(act, scene)));
            }
            card.getChildren().add(body);
        }
        return card;
    }

    private Node sceneActions(TheatreProjectLayer.TheatreAct act) {
        Button create = ActionButtonFactory.rail("Agregar escena", () -> showSceneEditor(act, null));
        create.setMaxWidth(Double.MAX_VALUE);
        Button edit = ActionButtonFactory.secondary("Editar escena", () -> {
            TheatreProjectLayer.Scene scene = selectedSceneInAct(act);
            if (scene != null) {
                showSceneEditor(act, scene);
            }
        });
        edit.setDisable(selectedSceneInAct(act) == null);
        edit.setMaxWidth(Double.MAX_VALUE);
        Button delete = ActionButtonFactory.danger("Eliminar escena", () -> {
            TheatreProjectLayer.Scene scene = selectedSceneInAct(act);
            if (scene == null) {
                return;
            }
            viewModel.deleteTheatreScene(scene.id());
            selectedScene.set(null);
            refreshActs();
        });
        delete.setDisable(selectedSceneInAct(act) == null);
        delete.setMaxWidth(Double.MAX_VALUE);

        VBox actions = new VBox(6, create, edit, delete);
        actions.getStyleClass().add("theatre-scene-actions");
        return actions;
    }

    private Node sceneCard(TheatreProjectLayer.TheatreAct act, TheatreProjectLayer.Scene scene) {
        boolean expanded = expandedSceneIds.contains(scene.id());
        boolean selected = selectedScene.get() != null && selectedScene.get().id().equals(scene.id());
        VBox card = new VBox(0);
        card.getStyleClass().add("theatre-scene-card");
        if (selected) {
            card.getStyleClass().add("theatre-scene-card-selected");
        }
        card.setMinWidth(0);
        card.setMaxWidth(Double.MAX_VALUE);

        Label chevron = new Label(expanded ? "\u25be" : "\u25b8");
        chevron.getStyleClass().add("theatre-act-chevron");

        Label title = new Label(scene.displayName());
        title.getStyleClass().add("theatre-scene-name");
        title.setWrapText(true);

        Button edit = titleEditButton("Editar nombre de la escena", () -> showSceneEditor(act, scene));
        Button delete = titleDeleteButton("Eliminar escena", () -> {
            viewModel.deleteTheatreScene(scene.id());
            selectedScene.set(null);
            expandedSceneIds.remove(scene.id());
            refreshActs();
        });
        Button addImage = titleImageButton("Agregar foto del personaje en esta escena", () -> chooseCharacterSceneImage(scene));
        addImage.setDisable(selectedCharacter.get() == null);

        HBox header = new HBox(7, chevron, title, edit, delete, addImage);
        header.getStyleClass().add("theatre-scene-header");
        header.setAlignment(Pos.CENTER_LEFT);
        header.setOnMouseClicked(event -> {
            selectedScene.set(scene);
            viewModel.focusTheatreScene(scene.id());
            toggleExpanded(expandedSceneIds, scene.id());
            refreshActs();
        });
        card.getChildren().add(header);

        if (expanded) {
            Label description = new Label(scene.notes().isBlank()
                    ? "Sin descripci\u00f3n."
                    : TheatreCharacterPresentation.excerpt(scene.notes(), 180));
            description.getStyleClass().add("theatre-scene-description");
            description.setWrapText(true);
            VBox body = new VBox(7, description, sceneCharacterImages(scene));
            body.getStyleClass().add("theatre-scene-body");
            card.getChildren().add(body);
        }
        return card;
    }

    private Button titleEditButton(String tooltip, Runnable action) {
        Button button = ActionButtonFactory.secondary("\u270e", tooltip, action);
        button.getStyleClass().add("theatre-title-edit-button");
        button.setMinSize(24, 24);
        button.setPrefSize(24, 24);
        button.setMaxSize(24, 24);
        button.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> event.consume());
        return button;
    }

    private Button titleDeleteButton(String tooltip, Runnable action) {
        Button button = ActionButtonFactory.danger("X", tooltip, action);
        button.getStyleClass().addAll("theatre-title-edit-button", "theatre-title-delete-button");
        button.setMinSize(24, 24);
        button.setPrefSize(24, 24);
        button.setMaxSize(24, 24);
        button.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> event.consume());
        return button;
    }

    private Button titleImageButton(String tooltip, Runnable action) {
        Button button = ActionButtonFactory.iconOnly(
                AppIcon.IMAGE,
                tooltip,
                action,
                "ui-action-button",
                "ui-action-button-secondary",
                "theatre-title-edit-button");
        button.setMinSize(24, 24);
        button.setPrefSize(24, 24);
        button.setMaxSize(24, 24);
        button.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> event.consume());
        return button;
    }

    private Node sceneCharacterImages(TheatreProjectLayer.Scene scene) {
        TheatreCharacterPresentation character = selectedCharacter.get();
        VBox list = new VBox(6);
        list.getStyleClass().add("theatre-scene-character-images");
        list.setMaxWidth(Double.MAX_VALUE);
        if (character == null) {
            list.getChildren().add(emptyNote("Selecciona un personaje para agregar fotos a esta escena."));
            return list;
        }

        List<TheatreProjectLayer.CharacterImage> images = viewModel.theatreCharacterSceneImages(character.id(), scene.id());
        if (images.isEmpty()) {
            list.getChildren().add(emptyNote("Sin fotos para " + character.displayName() + " en esta escena."));
            return list;
        }
        images.forEach(image -> list.getChildren().add(characterSceneImageCard(image)));
        return list;
    }

    private Node characterSceneImageCard(TheatreProjectLayer.CharacterImage image) {
        HBox card = new HBox(8);
        card.getStyleClass().add("theatre-scene-character-image-card");
        card.setAlignment(Pos.TOP_LEFT);
        card.setMaxWidth(Double.MAX_VALUE);

        StackPane previewBox = sceneImagePreview(image);

        Label note = new Label(image.notes().isBlank()
                ? "Sin nota."
                : TheatreCharacterPresentation.excerpt(image.notes(), 120));
        note.getStyleClass().add("theatre-scene-character-image-note");
        note.setWrapText(true);

        Button noteButton = ActionButtonFactory.secondary("Nota", () -> showCharacterSceneImageNoteEditor(image));
        Button replace = ActionButtonFactory.secondary("Reemplazar", () -> chooseReplacementCharacterSceneImage(image));
        Button viewFull = imageFullscreenButton(image);
        Button delete = ActionButtonFactory.danger("Eliminar", () -> {
            viewModel.deleteTheatreCharacterSceneImage(image.id());
            refreshAfterCharacterImageChange();
        });
        HBox actions = new HBox(6, noteButton, replace, viewFull, delete);
        actions.getStyleClass().add("theatre-scene-character-image-actions");
        actions.setAlignment(Pos.CENTER_LEFT);
        ResponsiveActionGroup.install(actions, 360, noteButton, replace, viewFull, delete);

        VBox copy = new VBox(5, note, actions);
        copy.setMinWidth(0);
        copy.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(copy, Priority.ALWAYS);

        card.getChildren().addAll(previewBox, copy);
        return card;
    }

    private StackPane sceneImagePreview(TheatreProjectLayer.CharacterImage image) {
        ImageView imageView = new ImageView();
        imageView.setFitWidth(92);
        imageView.setFitHeight(64);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        imageView.getStyleClass().add("theatre-scene-character-image-preview");

        Label empty = emptyImageLabel("Sin imagen");
        String uri = imageUri(image).orElse("");
        if (!uri.isBlank()) {
            try {
                Image loaded = new Image(uri, false);
                if (!loaded.isError()) {
                    imageView.setImage(loaded);
                    empty.setVisible(false);
                    empty.setManaged(false);
                }
            } catch (RuntimeException ignored) {
            }
        }

        StackPane box = new StackPane(imageView, empty);
        box.getStyleClass().add("theatre-scene-character-image-preview-box");
        StackPane.setAlignment(empty, Pos.CENTER);
        return box;
    }

    private Button imageFullscreenButton(TheatreProjectLayer.CharacterImage image) {
        Button button = ActionButtonFactory.iconOnly(
                AppIcon.FULLSCREEN,
                "Ver foto en pantalla completa",
                () -> showCharacterSceneImageFull(image),
                "ui-action-button",
                "ui-action-button-secondary",
                "theatre-scene-character-image-fullscreen-button");
        button.setMinSize(34, 34);
        button.setPrefSize(34, 34);
        button.setMaxSize(34, 34);
        button.setDisable(imageUri(image).isEmpty());
        return button;
    }

    private void showCharacterSceneImageFull(TheatreProjectLayer.CharacterImage image) {
        imageUri(image).ifPresent(uri -> ImageFullscreenViewer.show(
                uri,
                ownerWindow(),
                stylesheets(),
                "Foto de personaje",
                "No se pudo mostrar la foto completa",
                viewModel::reportUserVisibleError));
    }

    private void chooseCharacterSceneImage(TheatreProjectLayer.Scene scene) {
        TheatreCharacterPresentation character = selectedCharacter.get();
        if (character == null) {
            viewModel.updateStatusMessage("Selecciona un personaje antes de agregar fotos a la escena.");
            return;
        }
        File file = chooseImageFile("Agregar foto de " + character.displayName());
        if (file == null) {
            return;
        }
        try {
            viewModel.addTheatreCharacterSceneImage(character.id(), scene.id(), file.toPath());
            selectedScene.set(scene);
            expandedSceneIds.add(scene.id());
            refreshAfterCharacterImageChange();
        } catch (IOException | RuntimeException ex) {
            viewModel.reportUserVisibleError("No se pudo agregar la foto del personaje: " + safeMessage(ex));
        }
    }

    private void chooseReplacementCharacterSceneImage(TheatreProjectLayer.CharacterImage image) {
        File file = chooseImageFile("Reemplazar foto del personaje");
        if (file == null) {
            return;
        }
        try {
            viewModel.replaceTheatreCharacterSceneImage(image.id(), file.toPath());
            expandedSceneIds.add(image.sceneId());
            refreshAfterCharacterImageChange();
        } catch (IOException | RuntimeException ex) {
            viewModel.reportUserVisibleError("No se pudo reemplazar la foto del personaje: " + safeMessage(ex));
        }
    }

    private File chooseImageFile(String title) {
        FileChooser chooser = NativeSourceChooser.fileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Imagenes compatibles (*.png, *.jpg, *.jpeg, *.webp, *.gif)", "*.png", "*.jpg", "*.jpeg", "*.webp", "*.gif"),
                new FileChooser.ExtensionFilter("PNG recomendado (*.png)", "*.png")
        );
        return chooser.showOpenDialog(ownerWindow());
    }

    private void showCharacterSceneImageNoteEditor(TheatreProjectLayer.CharacterImage image) {
        String prompt = "Nota opcional:\n"
                + "Vestuario: chaqueta oscura con insignia metalica.\n"
                + "Estado visual: ropa manchada por aceite despues del accidente.\n"
                + "Continuidad: usar esta referencia solo para la escena del hangar.";
        TechnicalSheetTextArea note = new TechnicalSheetTextArea(image.notes(), prompt);

        Label title = new Label("Nota de foto");
        title.getStyleClass().add("theatre-character-dialog-title");
        title.setWrapText(true);

        Button save = ActionButtonFactory.primary("Guardar", () -> {
            viewModel.updateTheatreCharacterSceneImageNote(image.id(), note.text());
            expandedSceneIds.add(image.sceneId());
            refreshAfterCharacterImageChange();
            Stage stage = (Stage) note.getScene().getWindow();
            stage.close();
        });
        Button close = ActionButtonFactory.secondary("Cerrar", () -> {
            Stage stage = (Stage) note.getScene().getWindow();
            stage.close();
        });
        HBox actions = new HBox(8, save, close);
        actions.getStyleClass().add("theatre-character-dialog-actions");
        actions.setAlignment(Pos.CENTER_RIGHT);

        VBox root = new VBox(12, title, note, actions);
        root.getStyleClass().add("theatre-character-dialog-root");
        root.setPadding(new Insets(14));
        root.setFocusTraversable(true);
        VBox.setVgrow(note, Priority.ALWAYS);

        Stage stage = new Stage();
        Window owner = ownerWindow();
        if (owner != null) {
            stage.initOwner(owner);
        }
        stage.setTitle("Nota de foto");
        Scene sceneView = new Scene(root, 520, 360);
        if (getScene() != null) {
            sceneView.getStylesheets().addAll(getScene().getStylesheets());
        }
        stage.setScene(sceneView);
        stage.show();
        root.requestFocus();
    }

    private static String safeMessage(Exception ex) {
        String message = ex == null ? "" : ex.getMessage();
        return message == null || message.isBlank() ? "error desconocido" : message;
    }

    private void showActEditor(TheatreProjectLayer.TheatreAct act) {
        boolean creating = act == null;
        Label title = new Label(creating ? "Nuevo acto" : "Editar acto");
        title.getStyleClass().add("theatre-character-dialog-title");
        title.setWrapText(true);

        TextField name = StudioFormControls.textField(creating ? "Acto " + (viewModel.theatreActs().size() + 1) : act.displayName());
        name.getStyleClass().add("theatre-object-name-field");
        name.setPromptText("Nombre del acto");
        name.setMaxWidth(Double.MAX_VALUE);

        String prompt = "Ejemplo de descripci\u00f3n:\n"
                + "Prop\u00f3sito: presentar el mundo y el conflicto principal.\n"
                + "Ritmo: apertura visual amplia, avance gradual hacia el primer giro.\n"
                + "Continuidad: mantener el hangar como espacio dominante.";
        TechnicalSheetTextArea description = new TechnicalSheetTextArea(creating ? "" : act.notes(), prompt);

        Button accept = ActionButtonFactory.primary("Aceptar", () -> {
            if (creating) {
                viewModel.addTheatreAct(name.getText(), description.text());
                actExpansionInitialized = false;
            } else {
                viewModel.updateTheatreAct(act.id(), name.getText(), description.text());
                expandedActIds.add(act.id());
            }
            refreshActs();
            Stage stage = (Stage) description.getScene().getWindow();
            stage.close();
        });
        Button close = ActionButtonFactory.secondary("Cerrar", () -> {
            Stage stage = (Stage) description.getScene().getWindow();
            stage.close();
        });
        showNamedDescriptionDialog(creating ? "Nuevo acto" : "Editar acto", title, name, description, accept, close, 520, 420);
    }

    private void showSceneEditor(TheatreProjectLayer.TheatreAct act, TheatreProjectLayer.Scene scene) {
        boolean creating = scene == null;
        Label title = new Label(creating ? "Nueva escena en " + act.displayName() : "Editar escena");
        title.getStyleClass().add("theatre-character-dialog-title");
        title.setWrapText(true);

        TextField name = StudioFormControls.textField(creating ? "" : scene.displayName());
        name.getStyleClass().add("theatre-object-name-field");
        name.setPromptText("Nombre de la escena");
        name.setMaxWidth(Double.MAX_VALUE);

        String prompt = "Ejemplo de descripci\u00f3n:\n"
                + "Lugar: hangar principal al amanecer.\n"
                + "Ambiente: luz naranja, sombras largas y herramientas dispersas.\n"
                + "Prop\u00f3sito dram\u00e1tico: presentar el primer vuelo y la tensi\u00f3n entre los pilotos.\n"
                + "Continuidad visual: mantener el avi\u00f3n antiguo al fondo.";
        TechnicalSheetTextArea description = new TechnicalSheetTextArea(creating ? "" : scene.notes(), prompt);

        Button accept = ActionButtonFactory.primary("Aceptar", () -> {
            if (creating) {
                viewModel.addTheatreScene(act.id(), name.getText(), description.text());
                expandedActIds.add(act.id());
            } else {
                viewModel.updateTheatreScene(scene.id(), name.getText(), description.text());
            }
            refreshActs();
            Stage stage = (Stage) description.getScene().getWindow();
            stage.close();
        });
        Button close = ActionButtonFactory.secondary("Cerrar", () -> {
            Stage stage = (Stage) description.getScene().getWindow();
            stage.close();
        });
        showNamedDescriptionDialog(creating ? "Nueva escena" : "Editar escena", title, name, description, accept, close, 520, 420);
    }

    private void showNamedDescriptionDialog(
            String windowTitle,
            Label title,
            TextField name,
            TechnicalSheetTextArea description,
            Button accept,
            Button close,
            double width,
            double height) {
        HBox actions = new HBox(8, accept, close);
        actions.getStyleClass().add("theatre-character-dialog-actions");
        actions.setAlignment(Pos.CENTER_RIGHT);

        VBox root = new VBox(12, title, name, description, actions);
        root.getStyleClass().add("theatre-character-dialog-root");
        root.setPadding(new Insets(14));
        root.setFocusTraversable(true);
        VBox.setVgrow(description, Priority.ALWAYS);

        Stage stage = new Stage();
        Window owner = ownerWindow();
        if (owner != null) {
            stage.initOwner(owner);
        }
        stage.setTitle(windowTitle);
        Scene sceneView = new Scene(root, width, height);
        if (getScene() != null) {
            sceneView.getStylesheets().addAll(getScene().getStylesheets());
        }
        stage.setScene(sceneView);
        stage.show();
        root.requestFocus();
    }

    private TheatreProjectLayer.Scene selectedSceneInAct(TheatreProjectLayer.TheatreAct act) {
        TheatreProjectLayer.Scene scene = selectedScene.get();
        if (scene == null || !sceneActId(scene).equals(act.id())) {
            return null;
        }
        return scene;
    }

    private String sceneActId(TheatreProjectLayer.Scene scene) {
        String actId = scene.actId() == null ? "" : scene.actId().strip();
        return actId.isBlank() ? DEFAULT_ACT_ID : actId;
    }

    private String sceneCountLabel(int count) {
        return count == 1 ? "1 escena" : count + " escenas";
    }

    private void toggleExpanded(Set<String> expandedIds, String id) {
        if (!expandedIds.remove(id)) {
            expandedIds.add(id);
        }
    }

    private Node characterCard(TheatreCharacterPresentation character, boolean selected) {
        HBox card = new HBox(10);
        card.getStyleClass().add("theatre-character-card");
        if (selected) {
            card.getStyleClass().add("theatre-character-card-selected");
        }
        card.setAlignment(Pos.TOP_LEFT);
        card.setMinWidth(0);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setOnMouseClicked(event -> characters.getSelectionModel().select(character));

        StackPane imageBox = characterAvatarBox(character);

        Label name = new Label(character.displayName());
        name.getStyleClass().add("theatre-character-name");
        name.setWrapText(true);

        Label sheetStatus = new Label(character.sheetStatusLabel());
        sheetStatus.getStyleClass().add("theatre-character-sheet-status");
        sheetStatus.setWrapText(true);

        Label preview = new Label(character.cardPreview());
        preview.getStyleClass().add("theatre-character-preview");
        preview.setWrapText(true);

        Button description = ActionButtonFactory.rail("Ver descripci\u00f3n", () -> showDescriptionEditor(character));
        description.getStyleClass().add("theatre-character-description-button");
        Button assignVoice = ActionButtonFactory.secondary("Asignar voz", () -> showVoiceAssignmentDialog(character));
        assignVoice.getStyleClass().add("theatre-character-voice-button");

        HBox actions = new HBox(6, description, assignVoice);
        actions.getStyleClass().add("theatre-character-card-actions");

        VBox copy = new VBox(4, name, sheetStatus, preview, actions);
        copy.setMinWidth(0);
        copy.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(copy, Priority.ALWAYS);

        card.getChildren().addAll(imageBox, copy);
        return card;
    }

    private void showVoiceAssignmentDialog(TheatreCharacterPresentation character) {
        if (character == null) {
            viewModel.updateStatusMessage("Selecciona un personaje antes de asignar voz.");
            return;
        }
        List<VoiceAssignmentOption> choices = availableVoiceChoices(character);
        if (choices.isEmpty()) {
            viewModel.updateStatusMessage("No hay voces en la biblioteca para asignar a " + character.displayName() + ".");
            return;
        }
        Label title = new Label("Asignar voz a " + character.displayName());
        title.getStyleClass().add("theatre-character-dialog-title");
        title.setWrapText(true);

        ComboBox<VoiceAssignmentOption> voice = StudioFormControls.comboBox();
        voice.getItems().setAll(choices);
        voice.setMaxWidth(Double.MAX_VALUE);
        selectedVoiceChoice(character).ifPresentOrElse(
                selected -> choices.stream()
                        .filter(choice -> choice.voiceId().equals(selected.voiceProfileId()))
                        .findFirst()
                        .ifPresentOrElse(voice::setValue, () -> selectFirstSelectable(voice)),
                () -> selectFirstSelectable(voice));

        Label note = new Label("Las voces usadas por otros personajes o no disponibles con el motor activo aparecen bloqueadas.");
        note.getStyleClass().add("theatre-scene-description");
        note.setWrapText(true);
        Label status = new Label(voiceStatus(voice.getValue()));
        status.getStyleClass().add("theatre-scene-description");
        status.setWrapText(true);
        voice.valueProperty().addListener((obs, oldValue, newValue) -> status.setText(voiceStatus(newValue)));

        Button accept = ActionButtonFactory.primary("Aceptar", () -> {
            VoiceAssignmentOption selected = voice.getValue();
            if (selected == null || !selected.selectable()) {
                return;
            }
            viewModel.assignTheatreCharacterVoice(character.id(), character.displayName(), selected.voiceId());
            refreshCharacters();
            Stage stage = (Stage) voice.getScene().getWindow();
            stage.close();
        });
        accept.disableProperty().bind(voice.valueProperty().isNull()
                .or(javafx.beans.binding.Bindings.createBooleanBinding(
                        () -> voice.getValue() == null || !voice.getValue().selectable(),
                        voice.valueProperty())));
        Button cancel = ActionButtonFactory.secondary("Cancelar", () -> {
            Stage stage = (Stage) voice.getScene().getWindow();
            stage.close();
        });
        HBox actions = new HBox(8, accept, cancel);
        actions.getStyleClass().add("theatre-character-dialog-actions");
        actions.setAlignment(Pos.CENTER_RIGHT);

        VBox root = new VBox(12, title, voice, note, status, actions);
        root.getStyleClass().add("theatre-character-dialog-root");
        root.setPadding(new Insets(14));

        Stage stage = new Stage();
        Window owner = ownerWindow();
        if (owner != null) {
            stage.initOwner(owner);
        }
        stage.setTitle("Asignar voz");
        Scene sceneView = new Scene(root, 480, 220);
        if (getScene() != null) {
            sceneView.getStylesheets().addAll(getScene().getStylesheets());
        }
        stage.setScene(sceneView);
        stage.show();
        voice.requestFocus();
    }

    private List<VoiceAssignmentOption> availableVoiceChoices(TheatreCharacterPresentation character) {
        Set<String> usedByOthers = new LinkedHashSet<>();
        viewModel.theatreVoiceRoleAliases().stream()
                .filter(alias -> !alias.characterId().equals(character.id()))
                .map(TheatreProjectLayer.VoiceRoleAlias::voiceProfileId)
                .forEach(usedByOthers::add);
        String currentVoiceId = selectedVoiceChoice(character)
                .map(TheatreProjectLayer.VoiceRoleAlias::voiceProfileId)
                .orElse("");
        var roots = viewModel.runtimeWorkspace();
        VoiceAssignmentReadinessContext readiness = new VoiceAssignmentReadinessContext(
                roots.installationRoot(), roots.runtimeRoot(),
                viewModel.currentProjectDirectory().orElse(roots.runtimeRoot()));
        return viewModel.administrationWorkspace().voice().buildVoiceAssignmentOptions()
                .build(viewModel.activeVoiceLibraryProperty().get(), viewModel.audioEngineDescriptor(),
                        usedByOthers, currentVoiceId, readiness);
    }

    private Optional<TheatreProjectLayer.VoiceRoleAlias> selectedVoiceChoice(TheatreCharacterPresentation character) {
        return viewModel.theatreVoiceRoleAliasForCharacter(character.id());
    }

    private static void selectFirstSelectable(ComboBox<VoiceAssignmentOption> voice) {
        voice.getItems().stream()
                .filter(VoiceAssignmentOption::selectable)
                .findFirst()
                .ifPresentOrElse(voice::setValue, () -> voice.getSelectionModel().selectFirst());
    }

    private static String voiceStatus(VoiceAssignmentOption option) {
        if (option == null) {
            return "Selecciona una voz disponible.";
        }
        return option.status() + ". " + option.detail();
    }

    private StackPane characterAvatarBox(TheatreCharacterPresentation character) {
        ImageView imageView = emptyImageView();
        Label empty = emptyImageLabel("Sin imagen");
        firstSceneCharacterImageUri(character).ifPresent(uri -> {
            try {
                Image loaded = new Image(uri, false);
                if (!loaded.isError()) {
                    imageView.setImage(loaded);
                    empty.setVisible(false);
                    empty.setManaged(false);
                }
            } catch (RuntimeException ignored) {
            }
        });
        StackPane imageBox = new StackPane(imageView, empty);
        imageBox.getStyleClass().add("theatre-character-avatar-box");
        StackPane.setAlignment(empty, Pos.CENTER);
        return imageBox;
    }

    private Optional<String> firstSceneCharacterImageUri(TheatreCharacterPresentation character) {
        if (character == null) {
            return Optional.empty();
        }
        return viewModel.theatreScenes().stream().findFirst()
                .flatMap(scene -> viewModel.theatreCharacterSceneImages(character.id(), scene.id()).stream().findFirst())
                .flatMap(this::imageUri);
    }

    private Optional<String> imageUri(TheatreProjectLayer.CharacterImage image) {
        if (image == null) {
            return Optional.empty();
        }
        return viewModel.projectImageAssetUri(image.assetId())
                .filter(uri -> !uri.isBlank());
    }

    private List<String> stylesheets() {
        return getScene() == null ? List.of() : List.copyOf(getScene().getStylesheets());
    }

    private void refreshAfterCharacterImageChange() {
        refreshCharacters();
        refreshActs();
    }

    private ImageView emptyImageView() {
        ImageView imageView = new ImageView();
        imageView.setFitWidth(92);
        imageView.setFitHeight(64);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        imageView.getStyleClass().add("theatre-character-empty-image-view");
        return imageView;
    }

    private Label emptyImageLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("document-image-preview-empty");
        label.setWrapText(true);
        return label;
    }

    private Label railTitle(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("document-media-section-title");
        return label;
    }

    private Label emptyNote(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.getStyleClass().add("document-media-empty-note");
        return label;
    }

    private void showDescriptionEditor(TheatreCharacterPresentation character) {
        if (character == null) {
            return;
        }

        String prompt = "Ejemplo de ficha t\u00e9cnica:\n"
                + "Edad aparente: 45 a\u00f1os.\n"
                + "Apariencia: uniforme gastado, bigote prominente y postura r\u00edgida.\n"
                + "Personalidad: directo, orgulloso, protector y algo teatral.\n"
                + "Voz: grave, segura, con pausas cortas antes de ordenar.\n"
                + "Funci\u00f3n dram\u00e1tica: impulsa el conflicto y contrasta con el optimismo del copiloto.";
        TechnicalSheetTextArea description = new TechnicalSheetTextArea(character.description(), prompt);

        Label title = new Label("Ficha t\u00e9cnica de " + character.displayName());
        title.getStyleClass().add("theatre-character-dialog-title");
        title.setWrapText(true);

        Button save = ActionButtonFactory.primary("Guardar", () -> {
            viewModel.saveTheatreCharacterDescription(character.id(), character.displayName(), description.text());
            refresh();
            selectCharacter(character.id());
            Stage stage = (Stage) description.getScene().getWindow();
            stage.close();
        });
        Button close = ActionButtonFactory.secondary("Cerrar", () -> {
            Stage stage = (Stage) description.getScene().getWindow();
            stage.close();
        });
        HBox actions = new HBox(8, save, close);
        actions.getStyleClass().add("theatre-character-dialog-actions");
        actions.setAlignment(Pos.CENTER_RIGHT);

        VBox root = new VBox(12, title, description, actions);
        root.getStyleClass().add("theatre-character-dialog-root");
        root.setPadding(new Insets(14));
        root.setFocusTraversable(true);
        VBox.setVgrow(description, Priority.ALWAYS);

        Stage stage = new Stage();
        Window owner = ownerWindow();
        if (owner != null) {
            stage.initOwner(owner);
        }
        stage.setTitle("Ficha tecnica de " + character.displayName());
        Scene scene = new Scene(root, 560, 420);
        if (getScene() != null) {
            scene.getStylesheets().addAll(getScene().getStylesheets());
        }
        stage.setScene(scene);
        stage.show();
        root.requestFocus();
    }

    private Window ownerWindow() {
        return getScene() == null ? null : getScene().getWindow();
    }

    private final class CharacterCell extends ListCell<TheatreCharacterPresentation> {
        @Override
        protected void updateItem(TheatreCharacterPresentation character, boolean empty) {
            super.updateItem(character, empty);
            render(character, empty);
        }

        @Override
        public void updateSelected(boolean selected) {
            super.updateSelected(selected);
            render(getItem(), isEmpty());
        }

        private void render(TheatreCharacterPresentation character, boolean empty) {
            if (empty || character == null) {
                setText(null);
                setGraphic(null);
                return;
            }
            setText(null);
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            Node card = characterCard(character, isSelected());
            if (card instanceof Region region) {
                region.setPrefWidth(Math.max(180.0, getListView().getWidth() - 22.0));
            }
            setGraphic(card);
        }
    }

}

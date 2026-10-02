package com.marcosmoreiradev.docupodcaststudio.presentation.theatre;

import com.marcosmoreiradev.docupodcaststudio.presentation.dialogs.NativeSourceChooser;

import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioViewportControls;

import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioCollectionControls;

import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioFormControls;

import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.ActionButtonFactory;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.ResponsiveActionGroup;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.AppIcon;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.CollapsibleModuleSplitPane;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.ImageFullscreenViewer;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.SectionHeader;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.SidePanelToggleButton;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.DocuPodcastShellViewModel;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Theatre module that manages stage objects, props and scenery technical sheets. */
public final class TheatreObjectsPanel extends BorderPane {
    private static final String DEFAULT_ACT_ID = "ACT-ACTO-1";

    private final DocuPodcastShellViewModel viewModel;
    private final ObjectProperty<TheatreObjectPresentation> selectedObject = new SimpleObjectProperty<>();
    private final ObjectProperty<TheatreProjectLayer.Scene> selectedScene = new SimpleObjectProperty<>();
    private final ListView<TheatreObjectPresentation> objects = StudioCollectionControls.listView();
    private final VBox acts = new VBox(8);
    private final Set<String> expandedActIds = new LinkedHashSet<>();
    private final Set<String> expandedSceneIds = new LinkedHashSet<>();
    private final Label objectCount = new Label();
    private CollapsibleModuleSplitPane split;
    private boolean actExpansionInitialized;

    public TheatreObjectsPanel(DocuPodcastShellViewModel viewModel) {
        this.viewModel = viewModel;
        getStyleClass().add("theatre-objects-panel");

        VBox inspector = inspector();
        VBox rail = rail();
        split = new CollapsibleModuleSplitPane("Ficha", inspector, "Fichas de objetos", rail, 0.45);
        split.getStyleClass().add("theatre-objects-split");
        setCenter(split);

        refresh();
        viewModel.currentDocumentProperty().addListener((obs, oldValue, newValue) -> refresh());
        viewModel.currentScriptProperty().addListener((obs, oldValue, newValue) -> refresh());
        viewModel.dirtyProperty().addListener((obs, oldValue, newValue) -> refresh());
    }

    public BooleanProperty primaryVisibleProperty() {
        return split.primaryVisibleProperty();
    }

    public BooleanProperty secondaryVisibleProperty() {
        return split.secondaryVisibleProperty();
    }

    private VBox inspector() {
        VBox module = new VBox(10);
        module.getStyleClass().addAll("document-context-module", "theatre-object-inspector");
        module.setPadding(new Insets(10));
        module.setMaxHeight(Double.MAX_VALUE);
        module.setFillWidth(true);

        SectionHeader header = new SectionHeader(
                "Actos",
                "Organiza objetos por actos y escenas de la obra.");

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
        module.getStyleClass().addAll("document-media-rail", "theatre-object-rail");
        module.setMaxHeight(Double.MAX_VALUE);
        module.setFillWidth(true);

        objectCount.getStyleClass().add("document-media-counter-label");
        objectCount.setWrapText(true);

        objects.getStyleClass().addAll("document-media-virtual-list", "theatre-object-list");
        objects.setPlaceholder(emptyNote("Crea una ficha para listar objetos, utileria o escenografia."));
        objects.setCellFactory(list -> new ObjectCell());
        objects.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            selectedObject.set(newValue);
            refreshActs();
        });
        VBox.setVgrow(objects, Priority.ALWAYS);

        VBox content = new VBox(10, objectActions(), railTitle("Fichas de objetos"), objectCount, objects);
        content.getStyleClass().addAll("document-media-visual-list", "theatre-object-list-shell");
        VBox.setVgrow(content, Priority.ALWAYS);
        module.getChildren().add(content);
        return module;
    }

    private HBox objectActions() {
        Label label = new Label("Acciones de objetos");
        label.getStyleClass().add("document-media-action-label");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button create = ActionButtonFactory.rail("Nuevo objeto", () -> showObjectEditor(null));
        create.getStyleClass().add("theatre-object-create-button");
        Button hide = new SidePanelToggleButton(
                AppIcon.COLLAPSE,
                "Ocultar fichas de objetos",
                () -> split.secondaryVisibleProperty().set(false));
        HBox row = new HBox(8, label, spacer, hide, create);
        row.getStyleClass().add("document-media-actions");
        return row;
    }

    private void refresh() {
        refreshActs();
        refreshObjects();
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

    private void refreshObjects() {
        String selectedId = selectedObject.get() == null ? "" : selectedObject.get().id();
        List<TheatreObjectPresentation> next = viewModel.theatreObjects().stream()
                .map(TheatreObjectPresentation::from)
                .toList();
        objects.getItems().setAll(next);
        objectCount.setText(objectCountLabel(next.size()));
        selectObject(selectedId);
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

    private void selectObject(String selectedId) {
        if (selectedId == null || selectedId.isBlank()) {
            return;
        }
        for (TheatreObjectPresentation object : objects.getItems()) {
            if (object.id().equals(selectedId)) {
                objects.getSelectionModel().select(object);
                selectedObject.set(object);
                return;
            }
        }
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
            refreshAfterObjectImageChange();
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
            refreshAfterObjectImageChange();
        });
        Button addImage = titleImageButton("Agregar foto del objeto en esta escena", () -> chooseObjectSceneImage(scene));
        addImage.setDisable(selectedObject.get() == null);

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
            Label description = new Label(objectSceneDescription(scene));
            description.getStyleClass().add("theatre-scene-description");
            description.setWrapText(true);
            VBox body = new VBox(7, description, sceneObjectImages(scene));
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

    private String objectSceneDescription(TheatreProjectLayer.Scene scene) {
        TheatreObjectPresentation object = selectedObject.get();
        if (object != null) {
            return object.cardPreview();
        }
        return scene.notes().isBlank()
                ? "Sin descripcion."
                : TheatreCharacterPresentation.excerpt(scene.notes(), 180);
    }

    private Node sceneObjectImages(TheatreProjectLayer.Scene scene) {
        TheatreObjectPresentation object = selectedObject.get();
        VBox list = new VBox(6);
        list.getStyleClass().add("theatre-scene-character-images");
        list.setMaxWidth(Double.MAX_VALUE);
        if (object == null) {
            list.getChildren().add(emptyNote("Selecciona un objeto para agregar fotos a esta escena."));
            return list;
        }

        List<TheatreProjectLayer.ObjectImage> images = viewModel.theatreObjectSceneImages(object.id(), scene.id());
        if (images.isEmpty()) {
            list.getChildren().add(emptyNote("Sin fotos para " + object.displayName() + " en esta escena."));
            return list;
        }
        images.forEach(image -> list.getChildren().add(objectSceneImageCard(image)));
        return list;
    }

    private Node objectSceneImageCard(TheatreProjectLayer.ObjectImage image) {
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

        Button noteButton = ActionButtonFactory.secondary("Nota", () -> showObjectSceneImageNoteEditor(image));
        Button replace = ActionButtonFactory.secondary("Reemplazar", () -> chooseReplacementObjectSceneImage(image));
        Button viewFull = imageFullscreenButton(image);
        Button delete = ActionButtonFactory.danger("Eliminar", () -> {
            viewModel.deleteTheatreObjectSceneImage(image.id());
            refreshAfterObjectImageChange();
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

    private StackPane sceneImagePreview(TheatreProjectLayer.ObjectImage image) {
        ImageView imageView = new ImageView();
        imageView.setFitWidth(92);
        imageView.setFitHeight(64);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        imageView.getStyleClass().add("theatre-scene-character-image-preview");

        Label empty = emptyImageLabel("Sin imagen");
        imageUri(image).ifPresent(uri -> {
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

        StackPane box = new StackPane(imageView, empty);
        box.getStyleClass().add("theatre-scene-character-image-preview-box");
        StackPane.setAlignment(empty, Pos.CENTER);
        return box;
    }

    private Button imageFullscreenButton(TheatreProjectLayer.ObjectImage image) {
        Button button = ActionButtonFactory.iconOnly(
                AppIcon.FULLSCREEN,
                "Ver foto en pantalla completa",
                () -> showObjectSceneImageFull(image),
                "ui-action-button",
                "ui-action-button-secondary",
                "theatre-scene-character-image-fullscreen-button");
        button.setMinSize(34, 34);
        button.setPrefSize(34, 34);
        button.setMaxSize(34, 34);
        button.setDisable(imageUri(image).isEmpty());
        return button;
    }

    private void showObjectSceneImageFull(TheatreProjectLayer.ObjectImage image) {
        imageUri(image).ifPresent(uri -> ImageFullscreenViewer.show(
                uri,
                ownerWindow(),
                stylesheets(),
                "Foto de objeto",
                "No se pudo mostrar la foto completa",
                viewModel::reportUserVisibleError));
    }

    private void chooseObjectSceneImage(TheatreProjectLayer.Scene scene) {
        TheatreObjectPresentation object = selectedObject.get();
        if (object == null) {
            viewModel.updateStatusMessage("Selecciona un objeto antes de agregar fotos a la escena.");
            return;
        }
        File file = chooseImageFile("Agregar foto de " + object.displayName());
        if (file == null) {
            return;
        }
        try {
            viewModel.addTheatreObjectSceneImage(object.id(), scene.id(), file.toPath());
            selectedScene.set(scene);
            expandedSceneIds.add(scene.id());
            refreshAfterObjectImageChange();
        } catch (IOException | RuntimeException ex) {
            viewModel.reportUserVisibleError("No se pudo agregar la foto del objeto: " + safeMessage(ex));
        }
    }

    private void chooseReplacementObjectSceneImage(TheatreProjectLayer.ObjectImage image) {
        File file = chooseImageFile("Reemplazar foto del objeto");
        if (file == null) {
            return;
        }
        try {
            viewModel.replaceTheatreObjectSceneImage(image.id(), file.toPath());
            expandedSceneIds.add(image.sceneId());
            refreshAfterObjectImageChange();
        } catch (IOException | RuntimeException ex) {
            viewModel.reportUserVisibleError("No se pudo reemplazar la foto del objeto: " + safeMessage(ex));
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

    private void showObjectSceneImageNoteEditor(TheatreProjectLayer.ObjectImage image) {
        String prompt = "Nota opcional:\n"
                + "Material: metal envejecido, madera o tela.\n"
                + "Estado visual: limpio, roto, manchado o recien usado.\n"
                + "Continuidad: ubicacion exacta y escena donde debe verse.";
        TechnicalSheetTextArea note = new TechnicalSheetTextArea(image.notes(), prompt);

        Label title = new Label("Nota de foto de objeto");
        title.getStyleClass().add("theatre-character-dialog-title");
        title.setWrapText(true);

        Button save = ActionButtonFactory.primary("Guardar", () -> {
            viewModel.updateTheatreObjectSceneImageNote(image.id(), note.text());
            expandedSceneIds.add(image.sceneId());
            refreshAfterObjectImageChange();
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
        stage.setTitle("Nota de foto de objeto");
        Scene sceneView = new Scene(root, 520, 360);
        if (getScene() != null) {
            sceneView.getStylesheets().addAll(getScene().getStylesheets());
        }
        stage.setScene(sceneView);
        stage.show();
        root.requestFocus();
    }

    private Node objectCard(TheatreObjectPresentation object, boolean selected) {
        HBox card = new HBox(10);
        card.getStyleClass().add("theatre-object-card");
        if (selected) {
            card.getStyleClass().add("theatre-object-card-selected");
        }
        card.setAlignment(Pos.TOP_LEFT);
        card.setMinWidth(0);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setOnMouseClicked(event -> objects.getSelectionModel().select(object));

        StackPane imageBox = objectAvatarBox(object);

        Label name = new Label(object.displayName());
        name.getStyleClass().add("theatre-character-name");
        name.setWrapText(true);

        Label sheetStatus = new Label(object.sheetStatusLabel());
        sheetStatus.getStyleClass().add("theatre-character-sheet-status");
        sheetStatus.setWrapText(true);

        Label preview = new Label(object.cardPreview());
        preview.getStyleClass().add("theatre-character-preview");
        preview.setWrapText(true);

        Button description = ActionButtonFactory.rail("Ver ficha", () -> showObjectEditor(object));
        description.getStyleClass().add("theatre-character-description-button");

        VBox copy = new VBox(4, name, sheetStatus, preview, description);
        copy.setMinWidth(0);
        copy.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(copy, Priority.ALWAYS);

        card.getChildren().addAll(imageBox, copy);
        return card;
    }

    private StackPane objectAvatarBox(TheatreObjectPresentation object) {
        ImageView imageView = emptyImageView();
        Label empty = emptyImageLabel("Sin imagen");
        firstSceneObjectImageUri(object).ifPresent(uri -> {
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

    private Optional<String> firstSceneObjectImageUri(TheatreObjectPresentation object) {
        if (object == null) {
            return Optional.empty();
        }
        String selectedSceneId = selectedScene.get() == null ? "" : selectedScene.get().id();
        if (!selectedSceneId.isBlank()) {
            Optional<String> selectedSceneImage = viewModel.theatreObjectSceneImages(object.id(), selectedSceneId).stream()
                    .findFirst()
                    .flatMap(this::imageUri);
            if (selectedSceneImage.isPresent()) {
                return selectedSceneImage;
            }
        }
        return viewModel.theatreScenes().stream()
                .flatMap(scene -> viewModel.theatreObjectSceneImages(object.id(), scene.id()).stream())
                .findFirst()
                .flatMap(this::imageUri);
    }

    private Optional<String> imageUri(TheatreProjectLayer.ObjectImage image) {
        if (image == null) {
            return Optional.empty();
        }
        return viewModel.projectImageAssetUri(image.assetId())
                .filter(uri -> !uri.isBlank());
    }

    private ImageView emptyImageView() {
        ImageView imageView = new ImageView();
        imageView.setFitWidth(92);
        imageView.setFitHeight(64);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        imageView.getStyleClass().add("theatre-object-empty-image-view");
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

    private void showObjectEditor(TheatreObjectPresentation object) {
        String titleText = object == null ? "Nueva ficha de objeto" : "Ficha tecnica de " + object.displayName();
        String currentId = object == null ? "" : object.id();

        Label title = new Label(titleText);
        title.getStyleClass().add("theatre-character-dialog-title");
        title.setWrapText(true);

        TextField name = StudioFormControls.textField(object == null ? "" : object.displayName());
        name.getStyleClass().add("theatre-object-name-field");
        name.setPromptText("Nombre del objeto, utileria o elemento de escenografia");
        name.setMaxWidth(Double.MAX_VALUE);

        String prompt = "Ejemplo de ficha tecnica:\n"
                + "Categoria: utileria de mano.\n"
                + "Apariencia: metal envejecido, bordes gastados y etiqueta oxidada.\n"
                + "Uso en escena: aparece durante el despegue y debe verse en primer plano.\n"
                + "Continuidad: mantener siempre en la mesa izquierda del hangar.\n"
                + "Referencias visuales: fotografia frontal, detalle de textura y escala junto a un personaje.";
        TechnicalSheetTextArea description = new TechnicalSheetTextArea(object == null ? "" : object.description(), prompt);

        Button save = ActionButtonFactory.primary("Guardar", () -> {
            viewModel.saveTheatreObjectDescription(currentId, name.getText(), description.text());
            refresh();
            selectObjectByName(name.getText());
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
        stage.setTitle(object == null ? "Nueva ficha de objeto" : "Ficha tecnica de " + object.displayName());
        Scene scene = new Scene(root, 560, 460);
        if (getScene() != null) {
            scene.getStylesheets().addAll(getScene().getStylesheets());
        }
        stage.setScene(scene);
        stage.show();
        root.requestFocus();
    }

    private void selectObjectByName(String displayName) {
        String normalized = displayName == null ? "" : displayName.strip();
        for (TheatreObjectPresentation object : objects.getItems()) {
            if (object.displayName().equals(normalized)) {
                objects.getSelectionModel().select(object);
                selectedObject.set(object);
                return;
            }
        }
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

        String prompt = "Ejemplo de descripcion:\n"
                + "Proposito: agrupar las escenas donde aparecen estos objetos.\n"
                + "Continuidad: mantener utileria y escenografia consistente entre escenas.";
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

        String prompt = "Ejemplo de descripcion:\n"
                + "Lugar: hangar principal al amanecer.\n"
                + "Objetos clave: avion antiguo, herramientas, casco, mesa de trabajo.\n"
                + "Continuidad visual: conservar ubicacion y estado de cada objeto.";
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

    private String objectCountLabel(int count) {
        if (count <= 0) {
            return "Crea fichas para organizar utileria, escenografia y referencias visuales.";
        }
        return count + " objetos registrados. La lista muestra solo las fichas visibles para mantener fluido el documento.";
    }

    private String sceneCountLabel(int count) {
        return count == 1 ? "1 escena" : count + " escenas";
    }

    private void toggleExpanded(Set<String> expandedIds, String id) {
        if (!expandedIds.remove(id)) {
            expandedIds.add(id);
        }
    }

    private List<String> stylesheets() {
        return getScene() == null ? List.of() : List.copyOf(getScene().getStylesheets());
    }

    private void refreshAfterObjectImageChange() {
        refreshObjects();
        refreshActs();
    }

    private static String safeMessage(Exception ex) {
        String message = ex == null ? "" : ex.getMessage();
        return message == null || message.isBlank() ? "error desconocido" : message;
    }

    private Window ownerWindow() {
        return getScene() == null ? null : getScene().getWindow();
    }

    private final class ObjectCell extends ListCell<TheatreObjectPresentation> {
        @Override
        protected void updateItem(TheatreObjectPresentation object, boolean empty) {
            super.updateItem(object, empty);
            render(object, empty);
        }

        @Override
        public void updateSelected(boolean selected) {
            super.updateSelected(selected);
            render(getItem(), isEmpty());
        }

        private void render(TheatreObjectPresentation object, boolean empty) {
            if (empty || object == null) {
                setText(null);
                setGraphic(null);
                return;
            }
            setText(null);
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            Node card = objectCard(object, isSelected());
            if (card instanceof Region region) {
                region.setPrefWidth(Math.max(180.0, getListView().getWidth() - 22.0));
            }
            setGraphic(card);
        }
    }
}

package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import com.marcosmoreiradev.docupodcaststudio.domain.assignment.NarrativeLayerKind;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.ActionButtonFactory;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.ImageFullscreenViewer;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.SectionHeader;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.StableImageLoader;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioFormControls;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.DocuPodcastShellViewModel;
import com.marcosmoreiradev.docupodcaststudio.presentation.notification.ExceptionAlertPresenter;
import com.marcosmoreiradev.docupodcaststudio.presentation.notification.UserNotification;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ScrollPane;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

/** Contextual image module for visual layers attached to the selected document fragment. */
public final class DocumentImageContextPanel extends VBox {
    private final DocuPodcastShellViewModel viewModel;
    private final ImageView preview = new ImageView();
    private final Label emptyPreview = new Label("Sin imagen asociada");
    private final StringProperty selectedImageUri = new SimpleStringProperty("");
    private final ComboBox<CameraOption> cameraCombo = new ComboBox<>();
    private final CheckBox applyCameraPlane = new CheckBox("Aplicar plano");
    private final Label cameraSelectionNote = new Label("");
    private final ImageView backdropPreview = new ImageView();
    private final Label backdropPreviewLabel = new Label("Fondo no asignado\n(se aplica por defecto)");
    private boolean updatingCameraCombo;
    private boolean updatingApplyCameraPlane;
    private boolean previewRefreshScheduled;
    private boolean theatreVisualRefreshScheduled;
    private final ExceptionAlertPresenter alertPresenter = new ExceptionAlertPresenter();
    private final BooleanSupplier saveProjectRequest;

    public DocumentImageContextPanel(DocuPodcastShellViewModel viewModel) {
        this(viewModel, () -> false);
    }

    public DocumentImageContextPanel(DocuPodcastShellViewModel viewModel, BooleanSupplier saveProjectRequest) {
        this.viewModel = viewModel;
        this.saveProjectRequest = saveProjectRequest == null ? () -> false : saveProjectRequest;
        getStyleClass().add("document-context-module");
        setPadding(new Insets(10));
        setSpacing(10);
        setMaxHeight(Double.MAX_VALUE);
        setFillWidth(true);

        SectionHeader header = new SectionHeader(
                "Imagen",
                "Vincula una imagen al fragmento seleccionado.");

        Label selected = new Label();
        selected.getStyleClass().add("document-context-selected");
        selected.setWrapText(true);
        selected.textProperty().bind(viewModel.selectedDocumentRangeLabelProperty());

        configurePreview();
        configureBackdropPreview();
        StackPane previewBox = new StackPane(preview, emptyPreview);
        previewBox.getStyleClass().add("document-image-preview-box");
        StackPane.setAlignment(emptyPreview, Pos.CENTER);
        updatePreview(viewModel.selectedDocumentImageUri());

        Button choose = action("Elegir imagen", () -> chooseImageFromComputer(NarrativeLayerKind.IMAGE));
        Button chooseBridge = action("Elegir imagen puente", () -> chooseImageFromComputer(NarrativeLayerKind.BRIDGE_IMAGE));
        VBox cameraControls = cameraControls();
        CheckBox applyPlane = cameraApplicationControl(cameraControls);
        Button viewFull = ActionButtonFactory.rail("Pantalla completa (se pausa la reproduccion)", this::showFullImage);
        bindToSelectedImage(viewFull);
        Button remove = ActionButtonFactory.danger("Quitar imagen", viewModel::removeImageAssignmentForSelectedDocumentRange);
        bindToSelectedImage(remove);

        Label note = new Label("La imagen aparece aqui para revisar el fragmento y tambien queda listada en el panel Visual.");
        note.getStyleClass().add("document-context-note");
        note.setWrapText(true);

        VBox body = new VBox(8, header, selected, previewBox, choose, chooseBridge,
                applyPlane, cameraControls, backdropControls(), viewFull, remove, note);
        body.getStyleClass().add("document-context-body");
        body.setMaxWidth(Double.MAX_VALUE);
        ScrollPane scroll = new ScrollPane(body);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.getStyleClass().add("document-context-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        getChildren().add(scroll);
        installRefreshListeners();
        refreshCameraOptions();
        refreshApplyCameraPlane();
        refreshBackdropPreview();
    }

    private void configurePreview() {
        preview.setPreserveRatio(true);
        preview.setSmooth(true);
        preview.setFitWidth(220);
        preview.setFitHeight(130);
        preview.getStyleClass().add("document-image-preview");
        emptyPreview.getStyleClass().add("document-image-preview-empty");
    }

    private void configureBackdropPreview() {
        backdropPreview.setPreserveRatio(true);
        backdropPreview.setSmooth(true);
        backdropPreview.setFitWidth(220);
        backdropPreview.setFitHeight(124);
        backdropPreview.getStyleClass().add("document-image-preview");
        backdropPreviewLabel.getStyleClass().add("document-image-preview-empty");
        backdropPreviewLabel.setWrapText(true);
        backdropPreviewLabel.setAlignment(Pos.CENTER);
    }

    private void updatePreview(String uri) {
        String normalized = uri == null ? "" : uri.strip();
        if (normalized.equals(selectedImageUri.get())) {
            return;
        }
        selectedImageUri.set(normalized);
        StableImageLoader.shared().load(preview, normalized, 220, 130, true, state -> {
            boolean showPlaceholder = state == StableImageLoader.LoadState.EMPTY
                    || (state == StableImageLoader.LoadState.ERROR && preview.getImage() == null)
                    || (state == StableImageLoader.LoadState.LOADING && preview.getImage() == null);
            emptyPreview.setText(state == StableImageLoader.LoadState.ERROR
                    ? "No se pudo mostrar la imagen"
                    : state == StableImageLoader.LoadState.LOADING ? "Cargando imagen..." : "Sin imagen asociada");
            emptyPreview.setVisible(showPlaceholder);
            emptyPreview.setManaged(showPlaceholder);
        });
    }

    private void installRefreshListeners() {
        viewModel.selectedVisualFragmentImageUriProperty().addListener((obs, oldValue, newValue) -> schedulePreviewRefresh());
        viewModel.selectedVisualFragmentKeyProperty().addListener((obs, oldValue, newValue) -> scheduleSelectionRefresh());
        viewModel.documentMediaRevisionProperty().addListener((obs, oldValue, newValue) -> {
            schedulePreviewRefresh();
            scheduleTheatreVisualRefresh();
        });
    }

    private void scheduleSelectionRefresh() {
        schedulePreviewRefresh();
        scheduleTheatreVisualRefresh();
    }

    private void schedulePreviewRefresh() {
        if (previewRefreshScheduled) {
            return;
        }
        previewRefreshScheduled = true;
        Platform.runLater(() -> {
            previewRefreshScheduled = false;
            updatePreview(viewModel.selectedDocumentImageUri());
        });
    }

    private void scheduleTheatreVisualRefresh() {
        if (theatreVisualRefreshScheduled) {
            return;
        }
        theatreVisualRefreshScheduled = true;
        Platform.runLater(() -> {
            theatreVisualRefreshScheduled = false;
            refreshTheatreVisualControls();
        });
    }

    private void showFullImage() {
        String uri = selectedImageUri.get() == null ? "" : selectedImageUri.get().strip();
        AtomicBoolean pausedByFullscreen = new AtomicBoolean(false);
        ImageFullscreenViewer.show(
                uri,
                ownerWindow(),
                getScene() == null ? List.of() : List.copyOf(getScene().getStylesheets()),
                "Imagen completa",
                "No se pudo mostrar la imagen completa",
                viewModel::reportUserVisibleError,
                "Presiona Escape para salir y reanudar la reproduccion de la narracion.",
                () -> {
                    if (viewModel.playbackActiveForFullscreenPause()) {
                        viewModel.pausePlayback();
                        pausedByFullscreen.set(true);
                    }
                },
                () -> {
                    if (pausedByFullscreen.get()) {
                        viewModel.resumePlayback();
                    }
                });
    }

    private void chooseImageFromComputer(NarrativeLayerKind kind) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Elegir imagen para la selección");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Imágenes compatibles (*.png, *.jpg, *.jpeg, *.webp, *.gif)", "*.png", "*.jpg", "*.jpeg", "*.webp", "*.gif"),
                new FileChooser.ExtensionFilter("PNG recomendado (*.png)", "*.png")
        );
        File file = chooser.showOpenDialog(ownerWindow());
        if (file == null) {
            return;
        }
        importSelectedImage(file, kind);
    }

    private VBox cameraControls() {
        Label label = new Label("Tipo de plano");
        label.getStyleClass().add("document-context-field-label");
        cameraSelectionNote.getStyleClass().add("document-context-note");
        cameraSelectionNote.setWrapText(true);
        cameraCombo.setMaxWidth(Double.MAX_VALUE);
        StudioFormControls.combo(cameraCombo, "Tipo de plano heredado desde el fragmento seleccionado.");
        cameraCombo.getStyleClass().add("document-context-combo");
        cameraCombo.setButtonCell(cameraCell());
        cameraCombo.setCellFactory(list -> cameraCell());
        cameraCombo.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (updatingCameraCombo || newValue == null) {
                return;
            }
            try {
                viewModel.setTheatreCameraCueForSelectedSegment(newValue.id());
            } catch (IOException | RuntimeException ex) {
                viewModel.reportUserVisibleError("No se pudo aplicar el tipo de plano: " + ex.getMessage());
            }
        });
        Button catalog = ActionButtonFactory.secondary("Catalogo", this::showCameraCatalog);
        VBox box = new VBox(6, label, cameraCombo, cameraSelectionNote, catalog);
        box.getStyleClass().add("document-context-body");
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    private CheckBox cameraApplicationControl(VBox cameraControls) {
        applyCameraPlane.setSelected(true);
        applyCameraPlane.getStyleClass().addAll(StudioFormControls.FORM_CONTROL, StudioFormControls.FORM_TOGGLE);
        StudioFormControls.installTooltip(applyCameraPlane,
                "Usar el tipo de plano asignado como referencia visual para IA y como guia del lienzo.");
        applyCameraPlane.selectedProperty().addListener((obs, oldValue, newValue) -> {
            if (updatingApplyCameraPlane) {
                return;
            }
            try {
                viewModel.setSelectedTheatreApplyCameraPlane(Boolean.TRUE.equals(newValue));
            } catch (RuntimeException ex) {
                viewModel.reportUserVisibleError("No se pudo actualizar Aplicar plano: " + ex.getMessage());
                refreshApplyCameraPlane();
            }
        });
        cameraControls.visibleProperty().bind(applyCameraPlane.selectedProperty());
        cameraControls.managedProperty().bind(applyCameraPlane.selectedProperty());
        return applyCameraPlane;
    }

    private VBox backdropControls() {
        Label label = new Label("Fondo de escenario");
        label.getStyleClass().add("document-context-field-label");
        StackPane previewBox = new StackPane(backdropPreview, backdropPreviewLabel);
        previewBox.getStyleClass().add("document-image-preview-box");
        Button sceneBackdrop = action("Fondo de escenario", this::chooseStageBackdrop);
        Button clearBackdrop = ActionButtonFactory.danger(
                "Quitar fondo a partir de esta imagen",
                () -> {
                    try {
                        viewModel.clearStageBackdropFromSelectedSegment();
                    } catch (RuntimeException ex) {
                        viewModel.reportUserVisibleError("No se pudo quitar el fondo de escenario: " + ex.getMessage());
                    }
                });
        bindToSelection(clearBackdrop);
        VBox box = new VBox(6, label, previewBox, sceneBackdrop, clearBackdrop);
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    private void refreshBackdropPreview() {
        try {
            var backdrop = viewModel.selectedTheatreStageBackdropPreview();
            String uri = backdrop.uri();
            backdropPreviewLabel.setText(backdrop.displayName());
            StableImageLoader.shared().load(backdropPreview, uri, 220, 124, true, state -> {
                boolean showLabel = state == StableImageLoader.LoadState.EMPTY
                        || (state == StableImageLoader.LoadState.ERROR && backdropPreview.getImage() == null)
                        || (state == StableImageLoader.LoadState.LOADING && backdropPreview.getImage() == null);
                backdropPreviewLabel.setVisible(showLabel);
                backdropPreviewLabel.setManaged(showLabel);
            });
        } catch (RuntimeException ex) {
            StableImageLoader.shared().load(backdropPreview, "", 220, 124, true, ignored -> {});
            backdropPreviewLabel.setText("Fondo no asignado\n(se aplica por defecto)");
            backdropPreviewLabel.setVisible(true);
            backdropPreviewLabel.setManaged(true);
        }
    }

    private void refreshTheatreVisualControls() {
        refreshCameraOptions();
        refreshApplyCameraPlane();
        refreshBackdropPreview();
    }

    private ListCell<CameraOption> cameraCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(CameraOption item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                setText(null);
                ImageView thumb = new ImageView();
                thumb.setFitWidth(54);
                thumb.setFitHeight(32);
                thumb.setPreserveRatio(true);
                if (!item.uri().isBlank()) {
                    StableImageLoader.shared().load(thumb, item.uri(), 54, 32, true, ignored -> {});
                }
                Label text = new Label(item.displayName());
                text.setWrapText(true);
                HBox row = new HBox(8, thumb, text);
                row.setAlignment(Pos.CENTER_LEFT);
                setGraphic(row);
            }
        };
    }

    private void refreshCameraOptions() {
        List<CameraOption> options = viewModel.theatreCameraReferences().stream()
                .map(reference -> new CameraOption(reference.id(), reference.displayName(),
                        viewModel.theatreCameraImageUri(reference.id()).orElse("")))
                .toList();
        updatingCameraCombo = true;
        if (!cameraCombo.getItems().equals(options)) {
            cameraCombo.getItems().setAll(options);
        }
        String effectiveId = viewModel.selectedTheatreEffectiveCameraId();
        CameraOption selected = options.stream()
                .filter(option -> option.id().equals(effectiveId))
                .findFirst()
                .orElse(null);
        cameraCombo.getSelectionModel().select(selected);
        cameraSelectionNote.setText(selected == null
                ? "Plano de este fragmento visual: sin plano disponible."
                : "Plano de este fragmento visual: " + selected.displayName() + ".");
        cameraCombo.setPromptText(options.isEmpty() ? "Catalogo no disponible" : "Selecciona tipo de plano");
        updatingCameraCombo = false;
    }

    private void refreshApplyCameraPlane() {
        updatingApplyCameraPlane = true;
        applyCameraPlane.setSelected(viewModel.selectedTheatreApplyCameraPlane());
        updatingApplyCameraPlane = false;
    }

    private void showCameraCatalog() {
        List<CameraOption> options = cameraCombo.getItems().stream().toList();
        if (options.isEmpty()) {
            viewModel.reportUserVisibleError("No hay tipos de plano disponibles.");
            return;
        }
        Stage stage = new Stage();
        stage.setTitle("Catalogo de tipos de plano");
        Window owner = ownerWindow();
        if (owner != null) {
            stage.initOwner(owner);
        }
        stage.initModality(Modality.APPLICATION_MODAL);
        GridPane grid = new GridPane();
        grid.getStyleClass().add("theatre-camera-catalog-grid");
        grid.setHgap(44);
        grid.setVgap(48);
        grid.setPadding(new Insets(34));
        for (int i = 0; i < options.size(); i++) {
            CameraOption option = options.get(i);
            VBox card = cameraCatalogCard(option, stage);
            grid.add(card, i % 2, i / 2);
        }
        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setPrefViewportWidth(1360);
        scroll.setPrefViewportHeight(820);
        scroll.getStyleClass().add("theatre-camera-catalog-scroll");
        StackPane root = new StackPane(scroll);
        root.getStyleClass().add("theatre-camera-catalog-dialog");
        Scene scene = new Scene(root);
        if (getScene() != null) {
            scene.getStylesheets().addAll(getScene().getStylesheets());
        }
        scene.addEventFilter(KeyEvent.KEY_PRESSED, key -> {
            if (key.getCode() == KeyCode.ESCAPE) {
                key.consume();
                stage.close();
            }
        });
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.setFullScreenExitHint("");
        stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
        stage.setOnShown(event -> stage.setFullScreen(true));
        stage.showAndWait();
    }

    private VBox cameraCatalogCard(CameraOption option, Stage stage) {
        ImageView image = new ImageView();
        image.setFitWidth(600);
        image.setFitHeight(338);
        image.setPreserveRatio(true);
        if (!option.uri().isBlank()) {
            StableImageLoader.shared().load(image, option.uri(), 600, 338, true, ignored -> {});
        }
        Label title = new Label(option.displayName());
        title.setWrapText(true);
        title.getStyleClass().add("theatre-camera-catalog-title");
        VBox card = new VBox(8, image, title);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(18));
        card.getStyleClass().add("theatre-camera-catalog-card");
        card.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                event.consume();
                applyCameraOptionFromCatalog(option, stage);
            }
        });
        return card;
    }

    private void applyCameraOptionFromCatalog(CameraOption option, Stage stage) {
        try {
            viewModel.setTheatreCameraCueForSelectedSegment(option.id());
            stage.close();
        } catch (IOException | RuntimeException ex) {
            viewModel.reportUserVisibleError("No se pudo aplicar el tipo de plano: " + ex.getMessage());
        }
    }

    private void chooseStageBackdrop() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Elegir fondo de escenario desde este fragmento");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Imagenes compatibles (*.png, *.jpg, *.jpeg, *.webp)", "*.png", "*.jpg", "*.jpeg", "*.webp"),
                new FileChooser.ExtensionFilter("PNG recomendado (*.png)", "*.png")
        );
        File file = chooser.showOpenDialog(ownerWindow());
        if (file == null) {
            return;
        }
        try {
            viewModel.assignStageBackdropForSelectedSegment(file.toPath(), true);
        } catch (IOException | RuntimeException ex) {
            viewModel.reportUserVisibleError("No se pudo asignar el fondo de escenario: " + ex.getMessage());
        }
    }

    private void importSelectedImage(File file, NarrativeLayerKind kind) {
        try {
            viewModel.importImageForSelectedDocumentRange(file.toPath(), kind);
        } catch (IOException | RuntimeException ex) {
            String message = ex.getMessage() == null ? "" : ex.getMessage();
            if (message.contains("Guarda el proyecto")) {
                alertPresenter.show(UserNotification.warning(
                        "Guarda el proyecto",
                        "Para copiar la imagen dentro del proyecto y mantener rutas relativas, guarda primero la carpeta .docupodcast. Al aceptar se abrirá el diálogo de guardado."), ownerWindow());
                if (saveProjectRequest.getAsBoolean()) {
                    importSelectedImage(file, kind);
                    return;
                }
                viewModel.reportUserVisibleError("Guarda el proyecto antes de importar imágenes al documento.");
                return;
            }
            viewModel.reportUserVisibleError("No se pudo importar la imagen: " + message);
        }
    }

    private Window ownerWindow() {
        return getScene() == null ? null : getScene().getWindow();
    }

    private Button action(String text, Runnable handler) {
        Button button = ActionButtonFactory.rail(text, handler);
        bindToSelection(button);
        return button;
    }

    private void bindToSelection(Button button) {
        button.disableProperty().bind(Bindings.createBooleanBinding(() -> {
            String blockId = viewModel.selectedDocumentBlockIdProperty().get();
            String segmentId = viewModel.selectedScriptSegmentIdProperty().get();
            String visualSegmentId = viewModel.selectedVisualFragmentSegmentIdProperty().get();
            return (blockId == null || blockId.isBlank())
                    && (segmentId == null || segmentId.isBlank())
                    && (visualSegmentId == null || visualSegmentId.isBlank());
        }, viewModel.selectedDocumentBlockIdProperty(), viewModel.selectedScriptSegmentIdProperty(),
                viewModel.selectedVisualFragmentSegmentIdProperty()));
    }

    private void bindToSelectedImage(Button button) {
        button.disableProperty().bind(Bindings.createBooleanBinding(() -> {
            String uri = selectedImageUri.get();
            return uri == null || uri.isBlank();
        }, selectedImageUri));
    }

    private record CameraOption(String id, String displayName, String uri) {
        private CameraOption {
            displayName = displayName == null || displayName.isBlank() ? id : displayName;
            uri = uri == null ? "" : uri;
        }
    }
}

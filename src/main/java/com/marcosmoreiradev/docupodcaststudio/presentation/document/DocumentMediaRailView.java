package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import com.marcosmoreiradev.docupodcaststudio.presentation.dialogs.NativeDialogResponse;
import com.marcosmoreiradev.docupodcaststudio.presentation.dialogs.StudioMessageDialog;

import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioNavigationControls;

import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioCollectionControls;

import com.marcosmoreiradev.docupodcaststudio.ink.DrawingFeatureCatalog;
import com.marcosmoreiradev.docupodcaststudio.application.storyboard.TheatreVisualVariant;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackCue;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackCursor;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackManifest;
import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectMode;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.ActionButtonFactory;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.AppIcon;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.ImageFullscreenViewer;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.MediaThumbnailCard;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.SidePanelToggleButton;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.StableImageLoader;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.DocuPodcastShellViewModel;
import com.marcosmoreiradev.docupodcaststudio.presentation.theatre.TheatreFrameSketchDialog;
import com.marcosmoreiradev.docupodcaststudio.presentation.theatre.TheatreStoryboardOverviewDialog;
import javafx.geometry.Pos;
import javafx.css.PseudoClass;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.SemanticActionIcons;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Right document rail for sentence-level visual references.
 *
 * <p>Every narrable sentence/fragment receives a card, even when the user has not
 * associated an image yet. The card is logical until a virtual ListView cell renders it. The rail is virtualized so long documents do not materialize
 * thousands of JavaFX nodes while audio chunks are being generated.</p>
 */
public final class DocumentMediaRailView extends VBox {
    private static final PseudoClass VISUAL_SELECTED = PseudoClass.getPseudoClass("visual-selected");
    private static final PseudoClass PLAYBACK_ACTIVE = PseudoClass.getPseudoClass("playback-active");
    private final DocuPodcastShellViewModel viewModel;
    private final Runnable hideRailAction;
    private final Label fragmentCount = new Label();
    private final ListView<DocumentFragmentRailPresentation> storyboardItems = StudioCollectionControls.listView();
    private final Map<String, TheatreVisualVariant> inspectedVariants = new HashMap<>();
    private Button editFrameButton;
    private Button storyboardOverviewButton;

    public DocumentMediaRailView(DocuPodcastShellViewModel viewModel) {
        this(viewModel, null);
    }

    public DocumentMediaRailView(DocuPodcastShellViewModel viewModel, Runnable hideRailAction) {
        this.viewModel = viewModel;
        this.hideRailAction = hideRailAction;
        getStyleClass().add("document-media-rail");
        setSpacing(10);
        setMaxHeight(Double.MAX_VALUE);
        setFillWidth(true);

        fragmentCount.getStyleClass().add("document-media-counter-label");
        fragmentCount.setWrapText(true);

        storyboardItems.getStyleClass().add("document-media-virtual-list");
        storyboardItems.setPlaceholder(emptyNote("Prepara la lectura para listar cada frase del documento."));
        storyboardItems.setCellFactory(list -> new FragmentRailCell());
        VBox.setVgrow(storyboardItems, Priority.ALWAYS);

        VBox content = new VBox(10, mediaActions(), railTitle("Fragmentos visuales"), fragmentCount, storyboardItems);
        content.getStyleClass().add("document-media-visual-list");
        VBox.setVgrow(content, Priority.ALWAYS);
        getChildren().add(content);
        refreshContent();

        viewModel.currentStoryboardProperty().addListener((obs, oldValue, newValue) -> refreshContent());
        viewModel.currentScriptProperty().addListener((obs, oldValue, newValue) -> refreshContent());
        viewModel.currentDocumentProperty().addListener((obs, oldValue, newValue) -> refreshContent());
        viewModel.documentMediaRevisionProperty().addListener((obs, oldValue, newValue) -> refreshContent());
        viewModel.currentProjectModeProperty().addListener((obs, oldValue, newValue) -> refreshContent());
        viewModel.selectedScriptSegmentIdProperty().addListener((obs, oldValue, newValue) -> refreshActions());
        viewModel.selectedVisualFragmentKeyProperty().addListener((obs, oldValue, newValue) -> revealSelectedVisualFragment(newValue));
    }

    private VBox mediaActions() {
        Label label = new Label("Acciones visuales");
        label.getStyleClass().add("document-media-action-label");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button clear = ActionButtonFactory.danger("Borrar imágenes", viewModel::removeAllDocumentImages);
        clear.getStyleClass().add("document-media-clear-images");
        Tooltip.install(clear, new Tooltip("Eliminar todas las imágenes asignadas de todos los fragmentos y borrar los archivos de imagen del proyecto."));
        editFrameButton = ActionButtonFactory.secondary("Dibujar/editar frame", this::openSelectedFrameEditor);
        editFrameButton.setDisable(!viewModel.canEditTheatreStoryboardFrame());
        Tooltip.install(editFrameButton, new Tooltip("Abrir el editor de frame dibujado para el fragmento seleccionado."));
        storyboardOverviewButton = ActionButtonFactory.secondary("Mostrar storyboard completo", this::openStoryboardOverview);
        Tooltip.install(storyboardOverviewButton, new Tooltip("Abrir una vista completa de las imagenes configuradas por intervencion."));
        HBox row = new HBox(8, label, spacer);
        if (hideRailAction != null) {
            row.getChildren().add(new SidePanelToggleButton(
                    AppIcon.COLLAPSE,
                    "Ocultar acciones visuales",
                    hideRailAction));
        }
        VBox actionButtons = new VBox(8,
                fullWidthAction(editFrameButton),
                fullWidthAction(storyboardOverviewButton),
                fullWidthAction(clear));
        actionButtons.getStyleClass().add("document-media-actions-vertical");
        row.getStyleClass().add("document-media-actions");
        VBox group = new VBox(8, row, actionButtons);
        group.getStyleClass().add("document-media-actions");
        return group;
    }

    private static Button fullWidthAction(Button button) {
        button.setMaxWidth(Double.MAX_VALUE);
        button.setPrefWidth(230);
        button.setWrapText(true);
        return button;
    }

    private void refreshContent() {
        refreshActions();
        renderFragmentCards(viewModel.documentFragmentRailPresentations());
    }

    private void refreshActions() {
        if (editFrameButton != null) {
            editFrameButton.setVisible(theatreFramesEnabled());
            editFrameButton.setManaged(theatreFramesEnabled());
            editFrameButton.setDisable(!viewModel.canEditTheatreStoryboardFrame());
        }
        if (storyboardOverviewButton != null) {
            storyboardOverviewButton.setVisible(theatreFramesEnabled());
            storyboardOverviewButton.setManaged(theatreFramesEnabled());
            storyboardOverviewButton.setDisable(viewModel.currentScriptProperty().get() == null
                    || viewModel.currentScriptProperty().get().empty());
        }
    }

    private void renderFragmentCards(List<DocumentFragmentRailPresentation> fragments) {
        List<DocumentFragmentRailPresentation> safeFragments = fragments == null ? List.of() : fragments;
        Set<String> visibleUnits = safeFragments.stream().map(DocumentFragmentRailPresentation::unitId).collect(Collectors.toSet());
        inspectedVariants.keySet().retainAll(visibleUnits);
        DocumentRailReconciler.reconcile(storyboardItems.getItems(), safeFragments);
        fragmentCount.setText(fragmentCountLabel(safeFragments.size()));
    }

    private void revealSelectedVisualFragment(DocumentVisualFragmentKey key) {
        if (key == null || key.emptyKey()) {
            return;
        }
        for (int index = 0; index < storyboardItems.getItems().size(); index++) {
            if (key.matchesUnit(storyboardItems.getItems().get(index).unitId())) {
                storyboardItems.getSelectionModel().select(index);
                storyboardItems.scrollTo(index);
                return;
            }
        }
    }

    private String fragmentCountLabel(int count) {
        if (count <= 0) {
            return "Prepara la lectura para listar las frases visuales.";
        }
        return count + " fragmentos listados. La lista muestra solo las tarjetas visibles para mantener fluido el documento.";
    }

    private MediaThumbnailCard fragmentCard(DocumentFragmentRailPresentation fragment,
                                           DocumentFragmentRailPresentation previous,
                                           DocumentFragmentRailPresentation next,
                                           Runnable cardRefresh) {
        String description = fragment.preview().isBlank() ? "Frase sin vista previa." : fragment.preview();
        TheatreVisualVariant inspected = inspectedVariant(fragment);
        boolean available = variantAvailable(fragment, inspected);
        MediaThumbnailCard card = new MediaThumbnailCard(
                variantImageUri(fragment, inspected),
                variantPlaceholder(inspected),
                fragment.title(),
                fragment.relationLabel(),
                description,
                "document-media-frame-card " + fragment.cardStateCssClass(),
                null,
                theatreFramesEnabled() ? variantCategoryLabel(inspected) : "",
                thumbnailActionTooltip(fragment),
                theatreFramesEnabled() ? () -> handleThumbnailAction(fragment, cardRefresh) : null,
                available ? null : emptyVariantIcon(inspected),
                available ? "" : emptyVariantTooltip(inspected),
                available ? null : () -> handleEmptyVariantAction(fragment, inspected));
        installImageCopyMenu(card, fragment, previous, next);
        return card;
    }

    private void updateFragmentCard(MediaThumbnailCard card,
                                    DocumentFragmentRailPresentation fragment,
                                    DocumentFragmentRailPresentation previous,
                                    DocumentFragmentRailPresentation next,
                                    Runnable cardRefresh) {
        String description = fragment.preview().isBlank() ? "Frase sin vista previa." : fragment.preview();
        TheatreVisualVariant inspected = inspectedVariant(fragment);
        boolean available = variantAvailable(fragment, inspected);
        card.update(
                variantImageUri(fragment, inspected),
                variantPlaceholder(inspected),
                fragment.title(),
                fragment.relationLabel(),
                description,
                "document-media-frame-card " + fragment.cardStateCssClass(),
                null,
                theatreFramesEnabled() ? variantCategoryLabel(inspected) : "",
                thumbnailActionTooltip(fragment),
                theatreFramesEnabled() ? () -> handleThumbnailAction(fragment, cardRefresh) : null,
                available ? null : emptyVariantIcon(inspected),
                available ? "" : emptyVariantTooltip(inspected),
                available ? null : () -> handleEmptyVariantAction(fragment, inspected));
        installImageCopyMenu(card, fragment, previous, next);
    }

    private String thumbnailActionTooltip(DocumentFragmentRailPresentation fragment) {
        return "Ver siguiente variante: usuario, IA, lienzo o escena";
    }

    private static String variantCategoryLabel(TheatreVisualVariant variant) {
        return switch (variant) {
            case OFFICIAL -> "Usuario";
            case GENERATED -> "IA";
            case DRAWN -> "Lienzo";
            case SCENERY -> "Escena";
        };
    }

    private boolean theatreFramesEnabled() {
        return viewModel.currentProjectModeProperty().get() == ProjectMode.THEATRE_PRODUCTION;
    }

    private void handleThumbnailAction(DocumentFragmentRailPresentation fragment, Runnable cardRefresh) {
        viewModel.selectDocumentFragmentRailItem(fragment);
        TheatreVisualVariant current = inspectedVariant(fragment);
        TheatreVisualVariant[] variants = TheatreVisualVariant.values();
        TheatreVisualVariant next = variants[(current.ordinal() + 1) % variants.length];
        inspectedVariants.put(fragment.unitId(), next);
        if (variantAvailable(fragment, next)) {
            viewModel.activateTheatreStoryboardVisualVariant(fragment.segmentId(), next.metadataValue());
        } else if (next == TheatreVisualVariant.SCENERY) {
            viewModel.materializeTheatreSceneryVisualVariant(fragment.segmentId());
        }
        if (cardRefresh != null) {
            cardRefresh.run();
        }
    }

    private TheatreVisualVariant inspectedVariant(DocumentFragmentRailPresentation fragment) {
        return inspectedVariants.computeIfAbsent(fragment.unitId(),
                ignored -> TheatreVisualVariant.fromMetadata(fragment.activeVisualVariant()));
    }

    private static boolean variantAvailable(DocumentFragmentRailPresentation fragment, TheatreVisualVariant variant) {
        return switch (variant) {
            case OFFICIAL -> fragment.hasOfficialImage();
            case GENERATED -> fragment.hasGeneratedImage();
            case DRAWN -> fragment.hasDrawnFrame();
            case SCENERY -> fragment.hasSceneryImage();
        };
    }

    private static String variantImageUri(DocumentFragmentRailPresentation fragment, TheatreVisualVariant variant) {
        return switch (variant) {
            case OFFICIAL -> fragment.officialImageFileUri();
            case GENERATED -> fragment.generatedImageFileUri();
            case DRAWN -> fragment.drawnFrameFileUri();
            case SCENERY -> fragment.sceneryImageFileUri();
        };
    }

    private static String variantPlaceholder(TheatreVisualVariant variant) {
        return switch (variant) {
            case OFFICIAL -> "Imagen no asignada";
            case GENERATED -> "Imagen IA pendiente";
            case DRAWN -> "Boceto no disponible";
            case SCENERY -> "Composición de escena pendiente";
        };
    }

    private static AppIcon emptyVariantIcon(TheatreVisualVariant variant) {
        return variant == TheatreVisualVariant.DRAWN ? AppIcon.STORYBOARD : AppIcon.IMAGE;
    }

    private static String emptyVariantTooltip(TheatreVisualVariant variant) {
        return switch (variant) {
            case OFFICIAL -> "Seleccionar fragmento para asignar una imagen";
            case GENERATED -> "Abrir Generar con este fragmento seleccionado";
            case DRAWN -> "Dibujar o editar el frame de este fragmento";
            case SCENERY -> "Crear la composición con personajes y escenografía";
        };
    }

    private void handleEmptyVariantAction(DocumentFragmentRailPresentation fragment, TheatreVisualVariant variant) {
        viewModel.selectDocumentFragmentRailItem(fragment);
        if (variant == TheatreVisualVariant.GENERATED) {
            viewModel.showTheatreImageGenerationWorkspace();
        } else if (variant == TheatreVisualVariant.DRAWN) {
            openSelectedFrameEditor();
        } else if (variant == TheatreVisualVariant.SCENERY) {
            viewModel.materializeTheatreSceneryVisualVariant(fragment.segmentId());
        }
    }

    private void openSelectedFrameEditor() {
        var context = viewModel.selectedTheatreStoryboardFrameContext();
        if (context.isEmpty()) {
            return;
        }
        TheatreFrameSketchDialog dialog = new TheatreFrameSketchDialog(
                getScene() == null ? null : getScene().getWindow(),
                context.get(),
                this::openStoryboardOverview,
                viewModel.inkInputProviders().create(DrawingFeatureCatalog.THEATRE_FRAME),
                viewModel.drawingFeatures().require(DrawingFeatureCatalog.THEATRE_FRAME));
        try {
            dialog.showAndWait().ifPresent(result -> {
                try {
                    viewModel.saveTheatreStoryboardFrame(result.segmentId(), result.framePng(), result.inkStateJson(),
                            result.activateDrawn(), result.drawingVault());
                } catch (Exception ex) {
                    showFrameError(ex);
                }
            });
        } catch (RuntimeException ex) {
            showFrameError(ex);
        }
    }

    private void openStoryboardOverview() {
        TheatreStoryboardOverviewDialog.show(
                viewModel,
                getScene() == null ? null : getScene().getWindow(),
                getScene() == null ? List.of() : getScene().getStylesheets());
    }

    private void showFrameError(Exception ex) {
        Alert alert = NativeDialogResponse.alert(Alert.AlertType.ERROR);
        StudioMessageDialog.configure(
                alert,
                getScene() == null ? null : getScene().getWindow(),
                "Frame dibujado",
                "No se pudo guardar el frame",
                ex == null || ex.getMessage() == null ? "Error desconocido." : ex.getMessage(),
                StudioMessageDialog.technicalDetail(ex));
        alert.showAndWait();
    }

    private void installImageCopyMenu(MediaThumbnailCard card,
                                      DocumentFragmentRailPresentation fragment,
                                      DocumentFragmentRailPresentation previous,
                                      DocumentFragmentRailPresentation next) {
        MenuItem previousItem = new MenuItem("Asignar imagen al fragmento anterior");
        SemanticActionIcons.decorate(previousItem);
        previousItem.setDisable(previous == null || !fragment.imageReady());
        previousItem.setOnAction(event -> viewModel.copyFragmentImageToAdjacentFragment(fragment, previous));

        MenuItem nextItem = new MenuItem("Asignar imagen al fragmento posterior");
        SemanticActionIcons.decorate(nextItem);
        nextItem.setDisable(next == null || !fragment.imageReady());
        nextItem.setOnAction(event -> viewModel.copyFragmentImageToAdjacentFragment(fragment, next));

        ContextMenu menu = StudioNavigationControls.contextMenu(previousItem, nextItem);
        card.setOnContextMenuRequested(event -> {
            menu.show(card, event.getScreenX(), event.getScreenY());
            event.consume();
        });
    }

    private Optional<StackPane> intermediateFrameThumbnail(DocumentFragmentRailPresentation fragment,
                                                          DocumentFragmentRailPresentation next) {
        if (next == null || !theatreFramesEnabled()) {
            return Optional.empty();
        }
        return viewModel.theatreIntermediateFrameUri(fragment.segmentId(), next.segmentId())
                .map(uri -> {
                    ImageView image = new ImageView();
                    image.setPreserveRatio(true);
                    image.setSmooth(true);
                    image.setFitWidth(88);
                    image.setFitHeight(50);
                    StableImageLoader.shared().load(image, uri, 96, 54, false, null);
                    image.getStyleClass().add("document-media-intermediate-image");

                    StackPane thumb = new StackPane(image);
                    thumb.setAlignment(Pos.CENTER);
                    thumb.setMaxWidth(Double.MAX_VALUE);
                    thumb.getStyleClass().add("document-media-intermediate-thumb");
                    thumb.setOnMouseClicked(event -> {
                        if (event.getButton() == MouseButton.PRIMARY) {
                            ImageFullscreenViewer.show(
                                    uri,
                                    getScene() == null ? null : getScene().getWindow(),
                                    getScene() == null ? List.of() : getScene().getStylesheets(),
                                    "Frame inferido",
                                    "No se pudo mostrar el frame inferido",
                                    viewModel::reportUserVisibleError,
                                    "Escape para cerrar frame inferido",
                                    null,
                                    null);
                            event.consume();
                        }
                    });
                    return thumb;
                });
    }

    private String railPlaybackKey(PlaybackCursor cursor) {
        PlaybackManifest manifest = viewModel.currentPlaybackManifestProperty().get();
        if (manifest == null || manifest.emptyManifest() || cursor == null || cursor.segmentId().isBlank()) {
            return "";
        }
        return manifest.cueAt(cursor.positionSeconds())
                .filter(cue -> cue.segmentId().equals(cursor.segmentId()))
                .or(() -> manifest.cueForSegment(cursor.segmentId()))
                .map(PlaybackCue::unitId)
                .orElse(cursor.segmentId());
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

    private final class FragmentRailCell extends ListCell<DocumentFragmentRailPresentation> {
        private MediaThumbnailCard currentCard;
        private DocumentFragmentRailPresentation currentFragment;

        private FragmentRailCell() {
            setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 1 && !isEmpty() && getItem() != null) {
                    viewModel.selectDocumentFragmentRailItem(getItem());
                    event.consume();
                }
            });
            viewModel.selectedVisualFragmentKeyProperty().addListener((obs, oldValue, newValue) -> updateCardState());
            viewModel.playbackCursorProperty().addListener((obs, oldValue, newValue) -> {
                if (!railPlaybackKey(oldValue).equals(railPlaybackKey(newValue))) {
                    updateCardState();
                }
            });
        }

        @Override
        protected void updateItem(DocumentFragmentRailPresentation fragment, boolean empty) {
            super.updateItem(fragment, empty);
            if (empty || fragment == null) {
                currentFragment = null;
                currentCard = null;
                setText(null);
                setGraphic(null);
                setContextMenu(null);
                return;
            }
            boolean reusable = currentCard != null && currentFragment != null
                    && currentFragment.unitId().equals(fragment.unitId());
            currentFragment = fragment;
            if (reusable) {
                updateCurrentGraphic();
            } else {
                renderCurrentGraphic();
            }
        }

        private void renderCurrentGraphic() {
            DocumentFragmentRailPresentation fragment = currentFragment;
            if (fragment == null) {
                return;
            }
            int index = getIndex();
            List<DocumentFragmentRailPresentation> items = getListView().getItems();
            DocumentFragmentRailPresentation previous = index > 0 ? items.get(index - 1) : null;
            DocumentFragmentRailPresentation next = index + 1 < items.size() ? items.get(index + 1) : null;
            setText(null);
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            MediaThumbnailCard card = fragmentCard(fragment, previous, next, this::updateCurrentGraphic);
            currentCard = card;
            installCurrentGraphic(card, fragment, next);
        }

        private void updateCurrentGraphic() {
            DocumentFragmentRailPresentation fragment = currentFragment;
            MediaThumbnailCard card = currentCard;
            if (fragment == null || card == null) {
                return;
            }
            int index = getIndex();
            List<DocumentFragmentRailPresentation> items = getListView().getItems();
            DocumentFragmentRailPresentation previous = index > 0 ? items.get(index - 1) : null;
            DocumentFragmentRailPresentation next = index + 1 < items.size() ? items.get(index + 1) : null;
            updateFragmentCard(card, fragment, previous, next, this::updateCurrentGraphic);
            installCurrentGraphic(card, fragment, next);
        }

        private void installCurrentGraphic(MediaThumbnailCard card,
                                           DocumentFragmentRailPresentation fragment,
                                           DocumentFragmentRailPresentation next) {
            setText(null);
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            card.setMaxWidth(Double.MAX_VALUE);
            card.setPrefWidth(Math.max(160.0, getListView().getWidth() - 22.0));
            updateCardState();
            Optional<StackPane> intermediate = intermediateFrameThumbnail(fragment, next);
            if (intermediate.isPresent()) {
                VBox group = new VBox(5, card, intermediate.get());
                group.setAlignment(Pos.CENTER);
                group.setMaxWidth(Double.MAX_VALUE);
                setGraphic(group);
            } else {
                setGraphic(card);
            }
        }

        private void updateCardState() {
            if (currentCard == null || currentFragment == null) {
                return;
            }
            DocumentVisualFragmentKey selected = viewModel.selectedVisualFragmentKeyProperty().get();
            currentCard.pseudoClassStateChanged(VISUAL_SELECTED,
                    selected != null && selected.matchesUnit(currentFragment.unitId()));
            currentCard.pseudoClassStateChanged(PLAYBACK_ACTIVE,
                    currentFragment.unitId().equals(railPlaybackKey(viewModel.playbackCursorProperty().get())));
        }
    }
}

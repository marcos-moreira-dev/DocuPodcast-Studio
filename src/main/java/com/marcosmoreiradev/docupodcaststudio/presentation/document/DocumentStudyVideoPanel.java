package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import com.marcosmoreiradev.docupodcaststudio.presentation.dialogs.NativeSourceChooser;

import com.marcosmoreiradev.docupodcaststudio.presentation.dialogs.NativeDialogResponse;
import com.marcosmoreiradev.docupodcaststudio.presentation.dialogs.StudioMessageDialog;

import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioNavigationControls;

import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioViewportControls;

import com.marcosmoreiradev.docupodcaststudio.application.documentstudy.DocumentStudyVideoContentResolver;
import com.marcosmoreiradev.docupodcaststudio.application.document.DocumentContentProjection;
import com.marcosmoreiradev.docupodcaststudio.application.document.DocumentContentItem;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentMascotPosition;
import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentParagraphVisualAssignment;
import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentStudyClosingSlide;
import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentStudyVideoConfiguration;
import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentTableSlideConfiguration;
import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentVisualSource;
import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentVideoSlideConfiguration;
import com.marcosmoreiradev.docupodcaststudio.domain.study.SecondarySlideInclusionMode;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.SecondarySemanticReadingPolicy;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.ActionButtonFactory;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.ResponsiveActionGroup;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.AppIcon;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.CollapsibleModuleSplitPane;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.MediaThumbnailCard;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioFormControls;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.DocuPodcastShellViewModel;
import javafx.collections.FXCollections;
import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.SemanticActionIcons;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Configures format-neutral documentary slides for Word and admitted PDF content. */
public final class DocumentStudyVideoPanel extends BorderPane {
    private static final double COMPACT_WIDTH = 760.0;
    private static final PseudoClass SELECTED_MODE = PseudoClass.getPseudoClass("selected");

    private final DocuPodcastShellViewModel viewModel;
    private final DocumentStudyVideoContentResolver resolver = new DocumentStudyVideoContentResolver();
    private final VBox contentRows = new VBox(9);
    private final VBox selectedEditor = new VBox(10);
    private final Map<String, ContentRow> rowsByBlock = new LinkedHashMap<>();
    private final Map<String, ContentRow> rowsByContentId = new LinkedHashMap<>();
    private final Map<String, DocumentContentItem> contentByContentId = new LinkedHashMap<>();
    private final Map<String, DocumentVideoSlideConfiguration> configuredSlidesById = new LinkedHashMap<>();
    private final CollapsibleModuleSplitPane workspace;
    private final StackPane inspectorHost = new StackPane();
    private final Label inspectorModeTitle = sectionTitle("Editar contenido seleccionado");
    private final ScrollPane selectionInspector;
    private final ScrollPane contentScroll;
    private final DocumentStudyVideoSettingsPanel settingsPanel;
    private final CheckBox showOnlyIllustrations;
    private final CheckBox includeSecondarySlides;
    private final Button editModeButton;
    private final Button settingsModeButton;
    private List<DocumentStudyVideoContentResolver.Item> contentItems = List.of();
    private DocumentContentProjection renderedProjection;
    private String selectedBlockId = "";
    private boolean compactLayout;
    private InspectorMode activeInspectorMode = InspectorMode.SELECTION;

    public DocumentStudyVideoPanel(DocuPodcastShellViewModel viewModel) {
        this.viewModel = Objects.requireNonNull(viewModel, "viewModel");
        getStyleClass().add("document-study-video-panel");

        Label contentHeading = sectionTitle("Contenido del video");
        Label contentHint = hint("Una fila por contenido narrable o visual autorizado, en el orden original del documento.");
        Label selectedHeading = sectionTitle("Editar contenido seleccionado");
        Button addClosingSlide = ActionButtonFactory.primary(
                "Agregar nuevo parrafo final",
                "Anadir una diapositiva silenciosa con titulo, imagen y duracion propios.",
                this::addClosingSlide);
        showOnlyIllustrations = StudioFormControls.checkBox(
                "Mostrar solo im\u00e1genes ilustrativas",
                "Oculta el texto de todos los parrafos en el video, mantiene la narracion y el titulo general, "
                        + "y centra cada imagen ilustrativa.");
        showOnlyIllustrations.setSelected(false);
        showOnlyIllustrations.setOnAction(event ->
                setIllustrationOnlyForAll(showOnlyIllustrations.isSelected()));
        includeSecondarySlides = StudioFormControls.checkBox(
                "Incluir extras narrables en diapositiva propia",
                "Incluye tablas, ecuaciones, imágenes y extras autorizados. Si aún no tienen descripción, se muestran como una diapositiva silenciosa.");
        includeSecondarySlides.setOnAction(event -> saveConfiguration(
                configuration().withSecondarySlideInclusionMode(
                        includeSecondarySlides.isSelected()
                                ? SecondarySlideInclusionMode.INCLUDE_ALLOWED
                                : SecondarySlideInclusionMode.OMIT)));

        VBox inspectorBody = new VBox(12,
                selectedHeading,
                hint("Estas acciones afectan unicamente a la diapositiva seleccionada."),
                selectedEditor);
        inspectorBody.setPadding(new Insets(14));
        inspectorBody.setFillWidth(true);

        VBox contentBody = new VBox(10, contentHeading, contentHint, addClosingSlide,
                showOnlyIllustrations, includeSecondarySlides, contentRows);
        contentBody.setPadding(new Insets(14));
        contentBody.setFillWidth(true);

        selectionInspector = workspaceScroll(inspectorBody, "document-study-video-inspector-scroll");
        settingsPanel = new DocumentStudyVideoSettingsPanel(viewModel);
        inspectorHost.getChildren().setAll(selectionInspector);
        inspectorHost.getStyleClass().add("document-study-video-inspector-host");
        inspectorModeTitle.setId("document-study-video-inspector-title");
        VBox inspectorPane = new VBox(8, inspectorModeTitle, inspectorHost);
        inspectorPane.setFillWidth(true);
        inspectorPane.setPadding(new Insets(10, 0, 0, 0));
        VBox.setVgrow(inspectorHost, Priority.ALWAYS);
        contentScroll = workspaceScroll(contentBody, "document-study-video-content-scroll");
        workspace = new CollapsibleModuleSplitPane(
                "Editar selección",
                inspectorPane,
                "Contenido del video",
                contentScroll,
                0.43,
                false);
        workspace.getStyleClass().add("document-study-video-workspace-split");

        editModeButton = modeButton(AppIcon.IMAGE, "Editar selección", InspectorMode.SELECTION);
        settingsModeButton = modeButton(AppIcon.SETTINGS, "Ajustes del video", InspectorMode.SETTINGS);
        editModeButton.setId("document-study-video-edit-mode");
        settingsModeButton.setId("document-study-video-settings-mode");
        VBox modeRail = new VBox(10, editModeButton, settingsModeButton);
        modeRail.setAlignment(Pos.TOP_CENTER);
        modeRail.setPadding(new Insets(8, 6, 8, 6));
        modeRail.getStyleClass().add("document-study-video-mode-rail");
        HBox moduleWorkspace = new HBox(modeRail, workspace);
        HBox.setHgrow(workspace, Priority.ALWAYS);
        workspace.setMaxWidth(Double.MAX_VALUE);
        moduleWorkspace.getStyleClass().add("document-study-video-module-workspace");
        setCenter(moduleWorkspace);
        showInspectorMode(InspectorMode.SELECTION, false);
        workspace.primaryVisibleProperty().addListener((obs, oldValue, visible) -> {
            if (compactLayout && Boolean.TRUE.equals(visible)) workspace.secondaryVisibleProperty().set(false);
        });
        workspace.secondaryVisibleProperty().addListener((obs, oldValue, visible) -> {
            if (compactLayout && Boolean.TRUE.equals(visible)) workspace.primaryVisibleProperty().set(false);
        });
        widthProperty().addListener((obs, oldValue, value) -> updateResponsiveLayout(value.doubleValue()));

        viewModel.currentDocumentProperty().addListener((obs, oldValue, newValue) -> rebuildForDocument());
        viewModel.currentPreparedPdfSourceProperty().addListener((obs, oldValue, newValue) -> rebuildForDocument());
        viewModel.currentScriptProperty().addListener((obs, oldValue, newValue) -> rebuildForDocument());
        viewModel.currentProjectModeProperty().addListener((obs, oldValue, newValue) ->
                rebuildForDocument());
        viewModel.documentMediaRevisionProperty().addListener((obs, oldValue, newValue) -> refreshConfiguration());
        viewModel.documentarySourceVisualChangesProperty().addListener((obs, oldValue, changedIds) ->
                refreshSourceVisualRows(changedIds));
        viewModel.selectedDocumentBlockIdProperty().addListener((obs, oldValue, newValue) -> {
            String normalized = normalize(newValue);
            ContentRow row = rowForDocumentSelection(normalized);
            if (row != null) {
                selectRow(row.item().block().id(), false);
                scrollRowIntoView(row);
            } else if (!normalized.isBlank()) {
                clearVideoContentSelection();
            }
        });
        rebuildForDocument();
        Platform.runLater(() -> updateResponsiveLayout(getWidth()));
    }

    private Button modeButton(AppIcon icon, String label, InspectorMode mode) {
        Button button = ActionButtonFactory.sideDockRail(icon, label, () -> showInspectorMode(mode, true));
        button.setText("");
        button.setMaxWidth(Double.MAX_VALUE);
        button.getStyleClass().add("document-study-video-mode-button");
        return button;
    }

    private void showInspectorMode(InspectorMode mode, boolean reveal) {
        InspectorMode safeMode = mode == null ? InspectorMode.SELECTION : mode;
        activeInspectorMode = safeMode;
        inspectorModeTitle.setText(safeMode == InspectorMode.SELECTION
                ? "Editar contenido seleccionado"
                : "Ajustes del video");
        inspectorHost.getChildren().setAll(safeMode == InspectorMode.SELECTION
                ? selectionInspector
                : settingsPanel);
        editModeButton.pseudoClassStateChanged(SELECTED_MODE, safeMode == InspectorMode.SELECTION);
        settingsModeButton.pseudoClassStateChanged(SELECTED_MODE, safeMode == InspectorMode.SETTINGS);
        if (!reveal) return;
        workspace.primaryVisibleProperty().set(true);
        workspace.secondaryVisibleProperty().set(!compactLayout);
    }

    String activeInspectorModeLabel() {
        return activeInspectorMode == InspectorMode.SELECTION
                ? "Editar contenido seleccionado"
                : "Ajustes del video";
    }

    private static ScrollPane workspaceScroll(VBox content, String styleClass) {
        ScrollPane scroll = StudioViewportControls.scrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setMinWidth(0);
        scroll.getStyleClass().addAll("document-study-video-scroll", styleClass);
        return scroll;
    }

    private void rebuildForDocument() {
        renderedProjection = viewModel.currentDocumentContentProjection().orElse(null);
        rowsByBlock.clear();
        rowsByContentId.clear();
        contentByContentId.clear();
        contentRows.getChildren().clear();
        contentItems = List.of();
        if (!viewModel.documentaryVideoConfigurationAvailable() || renderedProjection == null) {
            selectedBlockId = "";
            selectedEditor.getChildren().setAll(hint(
                    "Abre un proyecto de estudio documental con una fuente Word o PDF y prepara su lectura."));
            return;
        }
        contentItems = resolver.resolve(renderedProjection, configuration(),
                        viewModel.documentListeningPreferences()
                                .secondarySemanticPolicy()).stream()
                .filter(item -> item.kind() != DocumentStudyVideoContentResolver.Kind.COVER)
                .toList();
        for (DocumentStudyVideoContentResolver.Item item : contentItems) {
            ContentRow row = createRow(item);
            rowsByBlock.put(item.block().id(), row);
            rowsByContentId.put(item.content().contentId(), row);
            contentByContentId.put(item.content().contentId(), item.content());
            rowsByContentId.putIfAbsent(item.block().id(), row);
            contentByContentId.putIfAbsent(item.block().id(), item.content());
            for (String sourceId : item.content().sourceIds()) {
                rowsByContentId.putIfAbsent(sourceId, row);
                contentByContentId.putIfAbsent(sourceId, item.content());
            }
            contentRows.getChildren().add(row.container());
        }
        String documentSelection = normalize(viewModel.selectedDocumentBlockIdProperty().get());
        ContentRow selectedRow = rowForDocumentSelection(documentSelection);
        if (selectedRow != null) selectedBlockId = selectedRow.item().block().id();
        else if (!rowsByBlock.containsKey(selectedBlockId)) selectedBlockId = "";
        refreshConfiguration();
        if (selectedRow != null) scrollRowIntoView(selectedRow);
        viewModel.materializeDocumentarySourceVisualsAsync(sourceVisualMaterializationOrder());
    }

    private List<String> sourceVisualMaterializationOrder() {
        java.util.LinkedHashSet<String> ordered = new java.util.LinkedHashSet<>();
        ContentRow selected = rowForDocumentSelection(
                normalize(viewModel.selectedDocumentBlockIdProperty().get()));
        if (selected != null
                && usesSourceCapture(selected.item().content().contentId())
                && hasOriginalSourceVisual(selected.item().content().contentId())) {
            ordered.add(selected.item().content().contentId());
        }
        contentItems.stream().map(item -> item.content().contentId())
                .filter(this::usesSourceCapture)
                .filter(this::hasOriginalSourceVisual)
                .forEach(ordered::add);
        return List.copyOf(ordered);
    }

    private ContentRow createRow(DocumentStudyVideoContentResolver.Item item) {
        MediaThumbnailCard card = new MediaThumbnailCard(
                "", item.kind() == DocumentStudyVideoContentResolver.Kind.TABLE ? "Tabla"
                        : hasOriginalSourceVisual(item.content().contentId())
                        ? originalSourceLabel(item.content().contentId()) + " pendiente" : "Sin imagen",
                "", "", "", "", null);
        VBox container = new VBox(card);
        container.setFillWidth(true);
        ContentRow row = new ContentRow(item, card, container);
        installContentMenu(row);
        return row;
    }

    private void refreshConfiguration() {
        rebuildConfigurationIndex();
        if (renderedProjection != null && viewModel.documentaryVideoConfigurationAvailable()) {
            List<String> resolvedIds = resolver.resolve(renderedProjection, configuration(),
                            viewModel.documentListeningPreferences()
                                    .secondarySemanticPolicy()).stream()
                    .filter(item -> item.kind() != DocumentStudyVideoContentResolver.Kind.COVER)
                    .map(item -> item.block().id())
                    .toList();
            if (!resolvedIds.equals(List.copyOf(rowsByBlock.keySet()))) {
                rebuildForDocument();
                return;
            }
        }
        for (ContentRow row : rowsByBlock.values()) updateRow(row);
        showOnlyIllustrations.setDisable(contentItems.stream().noneMatch(
                item -> item.kind() == DocumentStudyVideoContentResolver.Kind.PARAGRAPH));
        showOnlyIllustrations.setSelected(illustrationOnlyAppliedToAllParagraphs());
        SecondarySemanticReadingPolicy policy = viewModel
                .documentListeningPreferences().secondarySemanticPolicy();
        includeSecondarySlides.setDisable(false);
        includeSecondarySlides.setSelected(configuration().secondarySlideInclusionMode()
                != SecondarySlideInclusionMode.OMIT);
        refreshSelectedEditor();
    }

    private void rebuildConfigurationIndex() {
        configuredSlidesById.clear();
        for (DocumentVideoSlideConfiguration slide : configuration().contentSlides()) {
            configuredSlidesById.putIfAbsent(slide.contentId(), slide);
        }
    }

    /** Refreshes only thumbnails promoted by the background materializer. */
    private void refreshSourceVisualRows(java.util.Set<String> changedContentIds) {
        if (changedContentIds == null || changedContentIds.isEmpty()) return;
        rebuildConfigurationIndex();
        for (String contentId : changedContentIds) {
            ContentRow row = rowsByContentId.get(normalize(contentId));
            if (row != null) updateRow(row);
        }
    }

    private void updateResponsiveLayout(double width) {
        if (width <= 0) return;
        boolean nextCompact = width < COMPACT_WIDTH;
        if (nextCompact == compactLayout) return;
        compactLayout = nextCompact;
        workspace.secondaryVisibleProperty().set(true);
        workspace.primaryVisibleProperty().set(!compactLayout);
    }

    private void updateRow(ContentRow row) {
        DocumentBlock block = row.item().block();
        boolean selected = block.id().equals(selectedBlockId);
        boolean enabled = configuration().blockEnabled(block.id());
        String stateClass = rowStateClass(selected, enabled);
        if (row.item().kind() == DocumentStudyVideoContentResolver.Kind.TABLE) {
            int rows = integer(block.metadata().get("table.rowCount"));
            int columns = integer(block.metadata().get("table.columnCount"));
            double duration = row.item().durationSeconds();
            row.card().update("", "Tabla", enabled ? "Tabla" : "Tabla deshabilitada", "Bloque " + block.id(),
                    rows + " filas x " + columns + " columnas - " + seconds(duration),
                    stateClass, () -> selectRow(block.id(), true),
                    enabled ? "Tabla" : "Deshabilitado",
                    narrationIndicator(row), null, null, "", null);
            return;
        }
        if (row.item().kind() == DocumentStudyVideoContentResolver.Kind.CLOSING) {
            DocumentStudyClosingSlide slide = configuration().closingSlide(block.id()).orElseThrow();
            String imageUri = viewModel.resolveCurrentProjectAsset(slide.imageAssetId())
                    .map(path -> path.toUri().toString()).orElse("");
            String visibleTitle = slide.title().isBlank() ? "Sin titulo" : slide.title();
            row.card().update(imageUri, "Sin imagen", enabled ? "Diapositiva final" : "Diapositiva final deshabilitada",
                    visibleTitle, "Duracion: " + seconds(slide.durationSeconds()), stateClass,
                    () -> selectRow(block.id(), false), enabled ? "Final" : "Deshabilitado",
                    "", null, null, "", null);
            return;
        }
        DocumentParagraphVisualAssignment visual = assignment(block);
        String assetId = visual.activeImageAssetId();
        boolean sourceCapture = row.item().presentationMode()
                == com.marcosmoreiradev.docupodcaststudio.application.document
                .DocumentPresentationMode.SOURCE_CAPTURE;
        String imageUri = (sourceCapture || visual.illustrationOnly()
                ? viewModel.resolveCurrentProjectAsset(assetId)
                .or(() -> visual.activeSource() == DocumentVisualSource.NONE
                        ? configuredContent(block.id())
                        .map(com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentVideoSlideConfiguration::sourceVisualAssetId)
                        .flatMap(viewModel::resolveCurrentProjectAsset)
                        : Optional.empty())
                : Optional.<Path>empty())
                .map(path -> path.toUri().toString()).orElse("");
        String badge = switch (visual.activeSource()) {
            case IMPORTED -> "Importada";
            case DRAWN -> "Dibujo";
            case NONE -> originalSourceLabel(block.id());
        };
        String mascot = visual.mascotAssetId().isBlank() ? "" : " - mascota/logo asignado";
        String subtitle = visual.subtitle().isBlank() ? "" : " - subtitulo: " + visual.subtitle();
        String presentation = " - " + row.item().presentationMode().name()
                + (visual.illustrationOnly() ? " - solo ilustracion" : "");
        String visibleBadge = enabled ? badge : (badge.isBlank() ? "Deshabilitado" : badge + " / Deshabilitado");
        String emptyVisualLabel = com.marcosmoreiradev.docupodcaststudio.application.documentstudy
                .DocumentarySourceVisualPresentation.of(row.item().content(), !imageUri.isBlank()).label();
        row.card().update(imageUri, emptyVisualLabel,
                enabled ? row.item().presentationMode().name()
                        : row.item().presentationMode().name() + " deshabilitado",
                "Bloque " + block.id(), compactPreview(block.text()) + subtitle + mascot + presentation, stateClass,
                () -> selectRow(block.id(), true), visibleBadge,
                narrationIndicator(row), null, null, "", null);
    }

    private static String narrationIndicator(ContentRow row) {
        return row.item().content().secondarySemanticComponent()
                && (row.item().content().narratable()
                || hasAssociatedSemanticDescription(row.item().content()))
                ? "✓ Texto" : "";
    }

    private static boolean hasAssociatedSemanticDescription(
            com.marcosmoreiradev.docupodcaststudio.application.document.DocumentContentItem content) {
        return content.wordAnchor().map(anchor -> {
            String description = anchor.metadata().getOrDefault("description", "").strip();
            String state = anchor.metadata().getOrDefault("descriptionState", "").strip();
            String source = anchor.metadata().getOrDefault("descriptionSource", "").strip();
            return !description.isBlank()
                    && ("APPROVED".equalsIgnoreCase(state)
                    || "DRAFT".equalsIgnoreCase(state)
                    || "manual-user".equalsIgnoreCase(source)
                    || "qwen-word-image-v1".equalsIgnoreCase(source));
        }).orElse(false);
    }

    private ContentRow rowForDocumentSelection(String blockId) {
        String normalized = normalize(blockId);
        if (normalized.isBlank()) return null;
        ContentRow direct = rowsByBlock.get(normalized);
        if (direct != null) return direct;
        direct = rowsByContentId.get(normalized);
        if (direct != null) return direct;
        return rowsByBlock.values().stream()
                .filter(row -> row.item().content().matchesContentId(normalized)
                        || row.item().content().sourceIds().contains(normalized))
                .findFirst().orElse(null);
    }

    private void clearVideoContentSelection() {
        String previous = selectedBlockId;
        selectedBlockId = "";
        if (rowsByBlock.containsKey(previous)) updateRow(rowsByBlock.get(previous));
        refreshSelectedEditor();
    }

    private void scrollRowIntoView(ContentRow row) {
        if (row == null) return;
        Platform.runLater(() -> {
            if (contentScroll.getContent() == null || row.container().getScene() == null) return;
            javafx.geometry.Bounds rowBounds = contentScroll.getContent().sceneToLocal(
                    row.container().localToScene(row.container().getBoundsInLocal()));
            double contentHeight = contentScroll.getContent().getLayoutBounds().getHeight();
            double viewportHeight = contentScroll.getViewportBounds().getHeight();
            double scrollable = Math.max(0.0, contentHeight - viewportHeight);
            if (scrollable <= 0.0) return;
            double targetPixels = Math.max(0.0, rowBounds.getMinY() - 30.0);
            contentScroll.setVvalue(Math.min(1.0, targetPixels / scrollable));
        });
    }

    private void selectRow(String blockId, boolean synchronizeDocument) {
        String normalized = normalize(blockId);
        if (!rowsByBlock.containsKey(normalized)) return;
        String previous = selectedBlockId;
        selectedBlockId = normalized;
        if (!previous.equals(selectedBlockId)) {
            if (rowsByBlock.containsKey(previous)) updateRow(rowsByBlock.get(previous));
            updateRow(rowsByBlock.get(selectedBlockId));
            refreshSelectedEditor();
        }
        boolean sourceBlock = rowsByBlock.get(selectedBlockId).item().kind()
                != DocumentStudyVideoContentResolver.Kind.CLOSING;
        boolean blockBacked = renderedProjection != null
                && renderedProjection.format()
                != com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentFormat.PDF;
        if (synchronizeDocument && sourceBlock && blockBacked
                && !selectedBlockId.equals(viewModel.selectedDocumentBlockIdProperty().get())) {
            viewModel.selectDocumentBlock(selectedBlockId);
        }
    }

    private void refreshSelectedEditor() {
        selectedEditor.getChildren().clear();
        ContentRow row = rowsByBlock.get(selectedBlockId);
        if (row == null) {
            selectedEditor.getChildren().add(hint("Selecciona un parrafo o una tabla para configurar su diapositiva."));
            return;
        }
        boolean enabled = configuration().blockEnabled(row.item().block().id());
        Button availability = enabled
                ? ActionButtonFactory.danger("Deshabilitar este contenido", () -> setBlockEnabled(row.item().block(), false))
                : ActionButtonFactory.primary("Habilitar este contenido", () -> setBlockEnabled(row.item().block(), true));
        if (!enabled) {
            selectedEditor.getChildren().add(hint(
                    "Este contenido no se incluira en el video ni en la narracion hasta que se habilite."));
        }
        selectedEditor.getChildren().add(availability);
        if (row.item().kind() == DocumentStudyVideoContentResolver.Kind.TABLE) {
            buildTableEditor(row.item().block());
            buildVisualEditor(row.item().block(), false);
            appendSecondaryDurationEditor(row.item().block());
        } else if (row.item().kind() == DocumentStudyVideoContentResolver.Kind.CLOSING) {
            buildClosingEditor(configuration().closingSlide(row.item().block().id()).orElseThrow());
        } else {
            boolean paragraph = row.item().kind()
                    == DocumentStudyVideoContentResolver.Kind.PARAGRAPH;
            buildVisualEditor(row.item().block(), paragraph);
            if (row.item().content().secondarySemanticComponent()) {
                appendSecondaryDurationEditor(row.item().block());
            }
        }
    }

    private void buildVisualEditor(DocumentBlock block, boolean paragraph) {
        DocumentParagraphVisualAssignment visual = assignment(block);
        Label text = new Label(block.text());
        text.setWrapText(true);
        text.getStyleClass().add("document-study-video-selected-text");

        TextField subtitle = StudioFormControls.textInput(StudioFormControls.textField(visual.subtitle()),
                "Texto breve opcional que se muestra sobre el contenido de este parrafo.");
        subtitle.setPromptText("Subt\u00edtulo opcional");
        subtitle.setMaxWidth(Double.MAX_VALUE);
        subtitle.setOnAction(event -> saveSubtitleIfChanged(block, subtitle.getText()));
        subtitle.focusedProperty().addListener((obs, wasFocused, focused) -> {
            if (!focused) saveSubtitleIfChanged(block, subtitle.getText());
        });

        Button chooseImage = ActionButtonFactory.secondary("Elegir imagen", () -> chooseParagraphImage(block));
        Button draw = ActionButtonFactory.secondary("Dibujar/editar", () -> drawParagraphImage(block));
        Button restoreSource = ActionButtonFactory.secondary("Restaurar imagen original", () ->
                saveParagraph(assignment(block).withActiveSource(DocumentVisualSource.NONE)));
        restoreSource.setVisible(hasOriginalSourceVisual(block.id()));
        restoreSource.setManaged(restoreSource.isVisible());
        HBox imageActions = new HBox(8, chooseImage, draw, restoreSource);
        ResponsiveActionGroup.install(imageActions, 430, chooseImage, draw, restoreSource);
        HBox.setHgrow(chooseImage, Priority.ALWAYS);
        HBox.setHgrow(draw, Priority.ALWAYS);
        chooseImage.setMaxWidth(Double.MAX_VALUE);
        draw.setMaxWidth(Double.MAX_VALUE);

        ComboBox<DocumentVisualSource> source = StudioFormControls.combo(
                StudioFormControls.comboBox(FXCollections.observableArrayList(availableSources(visual))),
                "Elegir cual imagen se usara como principal sin eliminar la otra.");
        source.setConverter(sourceConverter(originalSourceLabel(block.id())));
        source.setValue(visual.activeSource());
        source.setMaxWidth(Double.MAX_VALUE);
        source.setOnAction(event -> {
            DocumentVisualSource value = source.getValue();
            if (value != null) saveParagraph(assignment(block).withActiveSource(value));
        });

        CheckBox applyImageToAll = StudioFormControls.checkBox(
                "Imagen ilustrativa para todo el video",
                "Copia la referencia de la imagen principal actual a cada parrafo. "
                        + "Despues puedes reemplazarla de forma independiente en cualquier parrafo.");
        applyImageToAll.setSelected(activeImageAppliedToAllParagraphs(visual));
        applyImageToAll.setDisable(visual.activeImageAssetId().isBlank());
        applyImageToAll.setVisible(paragraph);
        applyImageToAll.setManaged(paragraph);
        applyImageToAll.setOnAction(event -> {
            if (applyImageToAll.isSelected()) {
                copyImageToAllParagraphs(block);
            } else {
                applyImageToAll.setSelected(activeImageAppliedToAllParagraphs(assignment(block)));
            }
        });

        CheckBox illustrationOnly = StudioFormControls.checkBox(
                "Mostrar solo la imagen ilustrativa en este p\u00e1rrafo",
                "Oculta el texto de este parrafo en el video, mantiene su narracion y centra la imagen.");
        illustrationOnly.setSelected(visual.illustrationOnly());
        illustrationOnly.setVisible(paragraph);
        illustrationOnly.setManaged(paragraph);
        illustrationOnly.setOnAction(event -> saveParagraph(
                assignment(block).withIllustrationOnly(illustrationOnly.isSelected())));

        Button chooseMascot = ActionButtonFactory.secondary("Elegir mascota/logo", () -> chooseMascot(block));
        CheckBox applyMascotToAll = StudioFormControls.checkBox(
                "Mascota/logo para todo el video",
                "Copia la mascota o logo y su posicion a todos los parrafos. "
                        + "Despues puedes reemplazarlos de forma independiente.");
        applyMascotToAll.setSelected(mascotAppliedToAllParagraphs(visual));
        applyMascotToAll.setDisable(visual.mascotAssetId().isBlank());
        applyMascotToAll.setVisible(paragraph);
        applyMascotToAll.setManaged(paragraph);
        applyMascotToAll.setOnAction(event -> {
            if (applyMascotToAll.isSelected()) {
                copyMascotToAllParagraphs(block);
            } else {
                applyMascotToAll.setSelected(mascotAppliedToAllParagraphs(assignment(block)));
            }
        });
        Button clearMascot = ActionButtonFactory.danger("Quitar mascota/logo", () ->
                saveParagraph(assignment(block).withMascot("", assignment(block).mascotPosition())));
        clearMascot.setDisable(visual.mascotAssetId().isBlank());

        ComboBox<DocumentMascotPosition> position = StudioFormControls.combo(
                StudioFormControls.comboBox(FXCollections.observableArrayList(DocumentMascotPosition.values())),
                "Esquina inferior de la mascota o logo.");
        position.setConverter(positionConverter());
        position.setValue(visual.mascotPosition());
        position.setMaxWidth(Double.MAX_VALUE);
        position.setOnAction(event -> {
            if (position.getValue() != null && !assignment(block).mascotAssetId().isBlank()) {
                saveParagraph(assignment(block).withMascot(assignment(block).mascotAssetId(), position.getValue()));
            }
        });

        Button clearImage = ActionButtonFactory.danger("Dejar region de imagen vacia", () ->
                saveParagraph(assignment(block).withActiveSource(DocumentVisualSource.NONE)));
        clearImage.setDisable(visual.activeSource() == DocumentVisualSource.NONE);

        selectedEditor.getChildren().addAll(
                text,
                fieldLabel("Subt\u00edtulo"), subtitle,
                imageActions,
                fieldLabel("Imagen principal activa"), source,
                applyImageToAll,
                illustrationOnly,
                chooseMascot,
                applyMascotToAll,
                fieldLabel("Posicion de mascota/logo"), position,
                clearMascot,
                clearImage);
    }

    private void buildTableEditor(DocumentBlock block) {
        int rowCount = integer(block.metadata().get("table.rowCount"));
        int columnCount = integer(block.metadata().get("table.columnCount"));
        Label summary = new Label("Tabla " + rowCount + " x " + columnCount);
        summary.setWrapText(true);
        GridPane preview = tablePreview(block, rowCount, columnCount);
        selectedEditor.getChildren().addAll(summary, preview);
    }

    private void appendSecondaryDurationEditor(DocumentBlock block) {
        var slide = configuration().content(block.id()).orElseGet(() ->
                com.marcosmoreiradev.docupodcaststudio.domain.study
                        .DocumentVideoSlideConfiguration.empty(block.id()));
        double current = slide.durationSeconds() > 0.0
                ? slide.durationSeconds()
                : configuration().defaultSecondarySemanticDurationSeconds();
        Spinner<Double> duration = StudioFormControls.spinner(
                StudioFormControls.spinner(new SpinnerValueFactory.DoubleSpinnerValueFactory(
                        2.0, 60.0, current, 1.0)),
                "Duración específica cuando este componente no tenga audio.");
        duration.setEditable(true);
        Button save = ActionButtonFactory.primary("Guardar duracion", () ->
                saveConfiguration(configuration().withContent(
                        configuration().content(block.id()).orElseGet(() ->
                                com.marcosmoreiradev.docupodcaststudio.domain.study
                                        .DocumentVideoSlideConfiguration.empty(block.id()))
                                .withDuration(duration.getValue()))));
        Button restore = ActionButtonFactory.secondary("Usar duracion predeterminada", () ->
                saveConfiguration(configuration().withContent(
                        configuration().content(block.id()).orElseGet(() ->
                                com.marcosmoreiradev.docupodcaststudio.domain.study
                                        .DocumentVideoSlideConfiguration.empty(block.id()))
                                .withDuration(0.0))));
        selectedEditor.getChildren().addAll(
                fieldLabel("Duración de este componente"), duration, save, restore);
    }

    private void buildClosingEditor(DocumentStudyClosingSlide slide) {
        Label explanation = hint("Esta diapositiva no se narra. La musica de fondo continua durante toda su duracion.");
        TextField closingTitle = StudioFormControls.textInput(StudioFormControls.textField(slide.title()),
                "Titulo opcional de despedida o creditos.");
        closingTitle.setPromptText("Titulo opcional");
        closingTitle.setMaxWidth(Double.MAX_VALUE);
        Spinner<Double> duration = StudioFormControls.spinner(
                StudioFormControls.spinner(new SpinnerValueFactory.DoubleSpinnerValueFactory(
                        2.0, 60.0, slide.durationSeconds(), 1.0)),
                "Segundos durante los que se mostrara esta diapositiva.");
        duration.setEditable(true);

        Button save = ActionButtonFactory.primary("Guardar titulo y duracion", () ->
                saveClosingSlide(slide.withTitle(closingTitle.getText()).withDuration(duration.getValue())));
        Button chooseImage = ActionButtonFactory.secondary("Elegir imagen final", () -> chooseClosingImage(slide));
        Button clearImage = ActionButtonFactory.danger("Quitar imagen final", () ->
                saveClosingSlide(configuration().closingSlide(slide.id()).orElse(slide).withImage("")));
        clearImage.setDisable(slide.imageAssetId().isBlank());
        Button remove = ActionButtonFactory.danger("Eliminar esta diapositiva", () -> removeClosingSlide(slide.id()));

        selectedEditor.getChildren().addAll(
                explanation,
                fieldLabel("Titulo de la diapositiva"), closingTitle,
                fieldLabel("Duracion en segundos"), duration,
                save,
                chooseImage,
                clearImage,
                remove);
    }

    private GridPane tablePreview(DocumentBlock block, int rowCount, int columnCount) {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("document-study-video-table-preview");
        int visibleRows = Math.min(Math.max(rowCount, 1), 4);
        int visibleColumns = Math.min(Math.max(columnCount, 1), 3);
        for (int r = 0; r < visibleRows; r++) {
            for (int c = 0; c < visibleColumns; c++) {
                String key = r == 0 ? "table.header." + c : "table.cell." + r + "." + c;
                Label cell = new Label(block.metadata().getOrDefault(key, ""));
                cell.setWrapText(true);
                cell.setMaxWidth(Double.MAX_VALUE);
                cell.getStyleClass().add(r == 0 ? "document-study-video-table-header" : "document-study-video-table-cell");
                grid.add(cell, c, r);
                GridPane.setHgrow(cell, Priority.ALWAYS);
            }
        }
        return grid;
    }

    private void chooseParagraphImage(DocumentBlock block) {
        FileChooser chooser = imageChooser("Elegir imagen para el parrafo");
        File selected = chooser.showOpenDialog(getScene() == null ? null : getScene().getWindow());
        if (selected == null) return;
        try {
            ProjectAssetReference asset = viewModel.importDocumentaryVideoImage(selected.toPath());
            saveParagraph(assignment(block).withImportedImage(asset.id()));
        } catch (Exception ex) {
            showError("No se pudo asignar la imagen", ex);
        }
    }

    private void addClosingSlide() {
        String id = "DOC-CLOSING-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);
        DocumentStudyClosingSlide slide = DocumentStudyClosingSlide.empty(id);
        selectedBlockId = id;
        saveConfiguration(configuration().withClosingSlide(slide));
        rebuildForDocument();
    }

    private void chooseClosingImage(DocumentStudyClosingSlide slide) {
        FileChooser chooser = imageChooser("Elegir imagen para la diapositiva final");
        File selected = chooser.showOpenDialog(getScene() == null ? null : getScene().getWindow());
        if (selected == null) return;
        try {
            ProjectAssetReference asset = viewModel.importDocumentaryVideoImage(selected.toPath());
            DocumentStudyClosingSlide current = configuration().closingSlide(slide.id()).orElse(slide);
            saveClosingSlide(current.withImage(asset.id()));
        } catch (Exception ex) {
            showError("No se pudo asignar la imagen final", ex);
        }
    }

    private void saveClosingSlide(DocumentStudyClosingSlide slide) {
        saveConfiguration(configuration().withClosingSlide(slide));
    }

    private void removeClosingSlide(String id) {
        selectedBlockId = "";
        saveConfiguration(configuration().withoutClosingSlide(id));
        rebuildForDocument();
    }

    private void drawParagraphImage(DocumentBlock block) {
        DocumentParagraphVisualAssignment current = assignment(block);
        Path state = viewModel.resolveCurrentProjectRelativePath(current.drawnStateRelativePath()).orElse(null);
        DocumentParagraphSketchDialog dialog = new DocumentParagraphSketchDialog(
                getScene() == null ? null : getScene().getWindow(), block.text(), state,
                viewModel.drawingFeatures().require(com.marcosmoreiradev.docupodcaststudio.ink.DrawingFeatureCatalog.DOCUMENTARY_ILLUSTRATION),
                viewModel.inkInputProviders().create(com.marcosmoreiradev.docupodcaststudio.ink.DrawingFeatureCatalog.DOCUMENTARY_ILLUSTRATION));
        Optional<DocumentParagraphSketchDialog.Result> result = dialog.showAndWait();
        if (result.isEmpty()) return;
        try {
            var asset = viewModel.saveDocumentaryDrawing(block.id(), result.get().png(),
                    result.get().inkStateJson(), result.get().stagedSources());
            saveParagraph(assignment(block).withDrawnImage(asset.asset().id(), asset.stateRelativePath()));
        } catch (Exception ex) {
            showError("No se pudo guardar la ilustracion", ex);
        } finally {
            try { Files.deleteIfExists(result.get().png()); } catch (IOException ignored) { }
            for (Path source : result.get().stagedSources().values()) {
                try { Files.deleteIfExists(source); } catch (IOException ignored) { }
            }
        }
    }

    private void chooseMascot(DocumentBlock block) {
        FileChooser chooser = imageChooser("Elegir mascota o logo");
        File selected = chooser.showOpenDialog(getScene() == null ? null : getScene().getWindow());
        if (selected == null) return;
        try {
            ProjectAssetReference asset = viewModel.importDocumentaryVideoImage(selected.toPath());
            DocumentParagraphVisualAssignment current = assignment(block);
            saveParagraph(current.withMascot(asset.id(), current.mascotPosition()));
        } catch (Exception ex) {
            showError("No se pudo asignar la mascota o logo", ex);
        }
    }

    private void installContentMenu(ContentRow row) {
        DocumentBlock block = row.item().block();
        MenuItem availability = new MenuItem();
        availability.setOnAction(event ->
                setBlockEnabled(block, !configuration().blockEnabled(block.id())));
        MenuItem imageNext = new MenuItem("Usar imagen principal en el parrafo siguiente");
        SemanticActionIcons.decorate(imageNext);
        imageNext.setOnAction(event -> copyImageToNext(block));
        MenuItem imageAsMascot = new MenuItem("Establecer imagen como mascota/logo");
        SemanticActionIcons.decorate(imageAsMascot);
        imageAsMascot.setOnAction(event -> useImageAsMascot(block));
        MenuItem mascotNext = new MenuItem("Usar la misma mascota en el parrafo siguiente");
        SemanticActionIcons.decorate(mascotNext);
        mascotNext.setOnAction(event -> copyMascotToNext(block));
        MenuItem mascotRemaining = new MenuItem("Aplicar mascota a todos los parrafos restantes");
        SemanticActionIcons.decorate(mascotRemaining);
        mascotRemaining.setOnAction(event -> copyMascotToRemaining(block));
        MenuItem removeClosing = new MenuItem("Eliminar esta diapositiva");
        SemanticActionIcons.decorate(removeClosing);
        removeClosing.setOnAction(event -> removeClosingSlide(block.id()));
        ContextMenu menu = switch (row.item().kind()) {
            case PARAGRAPH -> StudioNavigationControls.contextMenu(availability, imageNext, imageAsMascot, mascotNext, mascotRemaining);
            case CLOSING -> StudioNavigationControls.contextMenu(availability, removeClosing);
            default -> StudioNavigationControls.contextMenu(availability);
        };
        row.card().setOnContextMenuRequested(event -> {
            boolean enabled = configuration().blockEnabled(block.id());
            availability.setText(enabled ? "Deshabilitar este contenido" : "Habilitar este contenido");
            if (row.item().kind() == DocumentStudyVideoContentResolver.Kind.PARAGRAPH) {
                DocumentParagraphVisualAssignment visual = assignment(block);
                imageNext.setDisable(!enabled || visual.activeImageAssetId().isBlank() || nextParagraph(block.id()).isEmpty());
                imageAsMascot.setDisable(!enabled || visual.activeImageAssetId().isBlank());
                mascotNext.setDisable(!enabled || visual.mascotAssetId().isBlank() || nextParagraph(block.id()).isEmpty());
                mascotRemaining.setDisable(!enabled || visual.mascotAssetId().isBlank() || remainingParagraphs(block.id()).isEmpty());
            }
            menu.show(row.card(), event.getScreenX(), event.getScreenY());
            event.consume();
        });
    }

    private void setBlockEnabled(DocumentBlock block, boolean enabled) {
        saveConfiguration(configuration().withBlockEnabled(block.id(), enabled));
    }

    private void copyImageToNext(DocumentBlock sourceBlock) {
        nextParagraph(sourceBlock.id()).ifPresent(target -> {
            DocumentParagraphVisualAssignment source = assignment(sourceBlock);
            saveParagraph(copyActiveImageReference(source, assignment(target)));
        });
    }

    private void copyImageToAllParagraphs(DocumentBlock sourceBlock) {
        DocumentParagraphVisualAssignment source = assignment(sourceBlock);
        if (source.activeImageAssetId().isBlank()) return;
        DocumentStudyVideoConfiguration next = configuration();
        for (DocumentStudyVideoContentResolver.Item item : contentItems) {
            if (item.kind() != DocumentStudyVideoContentResolver.Kind.PARAGRAPH) continue;
            next = next.withParagraph(copyActiveImageReference(source, assignment(item.block())));
        }
        saveConfiguration(next);
    }

    private boolean activeImageAppliedToAllParagraphs(DocumentParagraphVisualAssignment source) {
        if (source == null || source.activeImageAssetId().isBlank()) return false;
        boolean found = false;
        for (DocumentStudyVideoContentResolver.Item item : contentItems) {
            if (item.kind() != DocumentStudyVideoContentResolver.Kind.PARAGRAPH) continue;
            found = true;
            if (!sameActiveImageReference(source, assignment(item.block()))) return false;
        }
        return found;
    }

    private void setIllustrationOnlyForAll(boolean illustrationOnly) {
        DocumentStudyVideoConfiguration next = configuration();
        for (DocumentStudyVideoContentResolver.Item item : contentItems) {
            if (item.kind() != DocumentStudyVideoContentResolver.Kind.PARAGRAPH) continue;
            next = next.withParagraph(assignment(item.block()).withIllustrationOnly(illustrationOnly));
        }
        saveConfiguration(next);
    }

    private boolean illustrationOnlyAppliedToAllParagraphs() {
        boolean found = false;
        for (DocumentStudyVideoContentResolver.Item item : contentItems) {
            if (item.kind() != DocumentStudyVideoContentResolver.Kind.PARAGRAPH) continue;
            found = true;
            if (!assignment(item.block()).illustrationOnly()) return false;
        }
        return found;
    }

    static DocumentParagraphVisualAssignment copyActiveImageReference(
            DocumentParagraphVisualAssignment source,
            DocumentParagraphVisualAssignment destination) {
        if (source == null || destination == null) return destination;
        return switch (source.activeSource()) {
            case IMPORTED -> destination.withImportedImage(source.importedImageAssetId());
            case DRAWN -> destination.withDrawnImage(
                    source.drawnImageAssetId(), source.drawnStateRelativePath());
            case NONE -> destination;
        };
    }

    static boolean sameActiveImageReference(DocumentParagraphVisualAssignment left,
                                            DocumentParagraphVisualAssignment right) {
        if (left == null || right == null || left.activeSource() != right.activeSource()) return false;
        return switch (left.activeSource()) {
            case IMPORTED -> left.importedImageAssetId().equals(right.importedImageAssetId());
            case DRAWN -> left.drawnImageAssetId().equals(right.drawnImageAssetId())
                    && left.drawnStateRelativePath().equals(right.drawnStateRelativePath());
            case NONE -> true;
        };
    }

    private void useImageAsMascot(DocumentBlock block) {
        DocumentParagraphVisualAssignment visual = assignment(block);
        if (!visual.activeImageAssetId().isBlank()) {
            saveParagraph(visual.withMascot(visual.activeImageAssetId(), visual.mascotPosition()));
        }
    }

    private void copyMascotToNext(DocumentBlock sourceBlock) {
        nextParagraph(sourceBlock.id()).ifPresent(target -> {
            DocumentParagraphVisualAssignment source = assignment(sourceBlock);
            DocumentParagraphVisualAssignment destination = assignment(target)
                    .withMascot(source.mascotAssetId(), source.mascotPosition());
            saveParagraph(destination);
        });
    }

    private void copyMascotToRemaining(DocumentBlock sourceBlock) {
        DocumentParagraphVisualAssignment source = assignment(sourceBlock);
        DocumentStudyVideoConfiguration next = configuration();
        for (DocumentBlock target : remainingParagraphs(sourceBlock.id())) {
            next = next.withParagraph(assignment(target).withMascot(source.mascotAssetId(), source.mascotPosition()));
        }
        saveConfiguration(next);
    }

    private void copyMascotToAllParagraphs(DocumentBlock sourceBlock) {
        DocumentParagraphVisualAssignment source = assignment(sourceBlock);
        if (source.mascotAssetId().isBlank()) return;
        DocumentStudyVideoConfiguration next = configuration();
        for (DocumentStudyVideoContentResolver.Item item : contentItems) {
            if (item.kind() != DocumentStudyVideoContentResolver.Kind.PARAGRAPH) continue;
            next = next.withParagraph(assignment(item.block())
                    .withMascot(source.mascotAssetId(), source.mascotPosition()));
        }
        saveConfiguration(next);
    }

    private boolean mascotAppliedToAllParagraphs(DocumentParagraphVisualAssignment source) {
        if (source == null || source.mascotAssetId().isBlank()) return false;
        boolean found = false;
        for (DocumentStudyVideoContentResolver.Item item : contentItems) {
            if (item.kind() != DocumentStudyVideoContentResolver.Kind.PARAGRAPH) continue;
            found = true;
            if (!sameMascotReference(source, assignment(item.block()))) return false;
        }
        return found;
    }

    static boolean sameMascotReference(DocumentParagraphVisualAssignment left,
                                       DocumentParagraphVisualAssignment right) {
        return left != null && right != null
                && !left.mascotAssetId().isBlank()
                && left.mascotAssetId().equals(right.mascotAssetId())
                && left.mascotPosition() == right.mascotPosition();
    }

    private Optional<DocumentBlock> nextParagraph(String blockId) {
        List<DocumentBlock> remaining = remainingParagraphs(blockId);
        return remaining.isEmpty() ? Optional.empty() : Optional.of(remaining.get(0));
    }

    private List<DocumentBlock> remainingParagraphs(String blockId) {
        ArrayList<DocumentBlock> result = new ArrayList<>();
        boolean found = false;
        for (DocumentStudyVideoContentResolver.Item item : contentItems) {
            if (item.kind() != DocumentStudyVideoContentResolver.Kind.PARAGRAPH) continue;
            if (found) result.add(item.block());
            if (item.block().id().equals(blockId)) found = true;
        }
        return List.copyOf(result);
    }

    private DocumentParagraphVisualAssignment assignment(DocumentBlock block) {
        return configuration().content(block.id()).map(
                com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentVideoSlideConfiguration::visual)
                .or(() -> configuration().paragraph(block.id())).orElseGet(() -> new DocumentParagraphVisualAssignment(
                block.id(), fingerprint(block.text()), "", "", "", DocumentVisualSource.NONE,
                "", DocumentMascotPosition.BOTTOM_RIGHT));
    }

    private void saveParagraph(DocumentParagraphVisualAssignment assignment) {
        saveConfiguration(configuration().withParagraph(assignment));
    }

    private void saveSubtitleIfChanged(DocumentBlock block, String subtitle) {
        DocumentParagraphVisualAssignment current = assignment(block);
        DocumentParagraphVisualAssignment updated = current.withSubtitle(subtitle);
        if (!updated.subtitle().equals(current.subtitle())) saveParagraph(updated);
    }

    private void saveConfiguration(DocumentStudyVideoConfiguration configuration) {
        viewModel.updateDocumentaryVideoConfiguration(configuration);
    }

    private DocumentStudyVideoConfiguration configuration() {
        return viewModel.documentaryVideoConfiguration();
    }

    private static List<DocumentVisualSource> availableSources(DocumentParagraphVisualAssignment visual) {
        ArrayList<DocumentVisualSource> result = new ArrayList<>();
        result.add(DocumentVisualSource.NONE);
        if (!visual.importedImageAssetId().isBlank()) result.add(DocumentVisualSource.IMPORTED);
        if (!visual.drawnImageAssetId().isBlank()) result.add(DocumentVisualSource.DRAWN);
        return result;
    }

    private static StringConverter<DocumentVisualSource> sourceConverter(String originalLabel) {
        return new StringConverter<>() {
            @Override public String toString(DocumentVisualSource source) {
                if (source == null) return originalLabel.isBlank() ? "Sin imagen" : originalLabel;
                return switch (source) {
                    case NONE -> originalLabel.isBlank() ? "Sin imagen" : originalLabel;
                    case IMPORTED -> "Imagen importada";
                    case DRAWN -> "Dibujo";
                };
            }
            @Override public DocumentVisualSource fromString(String value) { return DocumentVisualSource.NONE; }
        };
    }

    private Optional<DocumentVideoSlideConfiguration> configuredContent(String contentId) {
        if (contentId == null || contentId.isBlank()) return Optional.empty();
        DocumentContentItem content = contentByContentId.get(contentId.strip());
        if (content != null) {
            DocumentVideoSlideConfiguration configured = configuredSlidesById.get(content.contentId());
            if (configured != null) return Optional.of(configured);
            for (String legacyId : content.legacyContentIds()) {
                configured = configuredSlidesById.get(legacyId);
                if (configured != null) return Optional.of(configured);
            }
        }
        return Optional.ofNullable(configuredSlidesById.get(contentId.strip()));
    }

    private boolean isPdfContent(String contentId) {
        if (contentId == null || contentId.isBlank()) return false;
        DocumentContentItem content = contentByContentId.get(contentId.strip());
        return content != null && content.pdfAnchor().isPresent();
    }

    private boolean hasOriginalSourceVisual(String contentId) {
        if (contentId == null || contentId.isBlank()) return false;
        DocumentContentItem content = contentByContentId.get(contentId.strip());
        return content != null && content.presentationMode()
                == com.marcosmoreiradev.docupodcaststudio.application.document
                .DocumentPresentationMode.SOURCE_CAPTURE
                && (content.pdfAnchor().isPresent()
                || content.wordAnchor().map(anchor -> !anchor.metadata()
                        .getOrDefault("embeddedImageBase64", "").isBlank()).orElse(false));
    }

    private boolean usesSourceCapture(String contentId) {
        if (contentId == null || contentId.isBlank()) return false;
        DocumentContentItem content = contentByContentId.get(contentId.strip());
        return content != null && content.presentationMode()
                == com.marcosmoreiradev.docupodcaststudio.application.document
                .DocumentPresentationMode.SOURCE_CAPTURE;
    }

    private String originalSourceLabel(String contentId) {
        if (!hasOriginalSourceVisual(contentId)) return "";
        return isPdfContent(contentId) ? "Imagen original PDF" : "Imagen original Word";
    }

    private static StringConverter<DocumentMascotPosition> positionConverter() {
        return new StringConverter<>() {
            @Override public String toString(DocumentMascotPosition position) {
                return position == DocumentMascotPosition.BOTTOM_LEFT ? "Inferior izquierda" : "Inferior derecha";
            }
            @Override public DocumentMascotPosition fromString(String value) { return DocumentMascotPosition.BOTTOM_RIGHT; }
        };
    }

    private static FileChooser imageChooser(String title) {
        FileChooser chooser = NativeSourceChooser.fileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().setAll(
                new FileChooser.ExtensionFilter("Imagenes compatibles", "*.png", "*.jpg", "*.jpeg", "*.webp", "*.gif"),
                new FileChooser.ExtensionFilter("PNG recomendado", "*.png"));
        return chooser;
    }

    private void showError(String header, Exception ex) {
        Alert alert = NativeDialogResponse.alert(Alert.AlertType.ERROR);
        StudioMessageDialog.configure(
                alert,
                getScene() == null ? null : getScene().getWindow(),
                "Video documental",
                header,
                rootMessage(ex),
                StudioMessageDialog.technicalDetail(ex));
        alert.showAndWait();
    }

    private static Label sectionTitle(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("document-study-video-section-title");
        return label;
    }

    private static Label fieldLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("document-study-video-field-label");
        return label;
    }

    private static Label hint(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.getStyleClass().add("document-study-video-hint");
        return label;
    }

    private static String fingerprint(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(normalize(text).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            return Integer.toHexString(normalize(text).hashCode());
        }
    }

    private static int integer(String value) {
        try { return Math.max(0, Integer.parseInt(value)); } catch (Exception ignored) { return 0; }
    }

    private static String seconds(double value) {
        return String.format(Locale.ROOT, "%.1f s", value);
    }

    private static String compactPreview(String text) {
        String normalized = normalize(text);
        int limit = 190;
        if (normalized.length() <= limit) return normalized;
        return normalized.substring(0, limit).stripTrailing() + "...";
    }

    private static String rowStateClass(boolean selected, boolean enabled) {
        String selectedClass = selected ? "document-study-video-card-selected" : "";
        String disabledClass = enabled ? "" : "document-study-video-card-disabled";
        return (selectedClass + " " + disabledClass).strip();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }

    private static String rootMessage(Throwable error) {
        Throwable cursor = error;
        while (cursor != null && cursor.getCause() != null && cursor.getCause() != cursor) cursor = cursor.getCause();
        String message = cursor == null ? "Error desconocido." : cursor.getMessage();
        return message == null || message.isBlank() ? String.valueOf(cursor) : message;
    }

    private record ContentRow(DocumentStudyVideoContentResolver.Item item,
                              MediaThumbnailCard card,
                              VBox container) { }

    private enum InspectorMode {
        SELECTION,
        SETTINGS
    }
}

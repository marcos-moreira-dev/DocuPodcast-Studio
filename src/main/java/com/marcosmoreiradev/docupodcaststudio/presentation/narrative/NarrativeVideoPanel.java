package com.marcosmoreiradev.docupodcaststudio.presentation.narrative;

import com.marcosmoreiradev.docupodcaststudio.application.narrative.NarrativeDocumentContext;
import com.marcosmoreiradev.docupodcaststudio.application.visual.VisualClipGenerationProfile;
import com.marcosmoreiradev.docupodcaststudio.application.visual.VisualGenerationProfile;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.narrative.NarrativeContextReference;
import com.marcosmoreiradev.docupodcaststudio.domain.narrative.NarrativeContextRole;
import com.marcosmoreiradev.docupodcaststudio.domain.narrative.NarrativeParagraphTake;
import com.marcosmoreiradev.docupodcaststudio.domain.narrative.NarrativeProjectLayer;
import com.marcosmoreiradev.docupodcaststudio.domain.narrative.NarrativeVideoConfiguration;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.ActionButtonFactory;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.AppIcon;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.CollapsibleModuleSplitPane;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.MediaThumbnailCard;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioFormControls;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.DocuPodcastShellViewModel;
import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Narrative-video production surface backed by one take per narratable Word paragraph. */
public final class NarrativeVideoPanel extends BorderPane {
    private static final double COMPACT_WIDTH = 720.0;
    private static final PseudoClass SELECTED = PseudoClass.getPseudoClass("selected");

    private final DocuPodcastShellViewModel viewModel;
    private final VBox takeRows = new VBox(9);
    private final Map<String, TakeRow> rows = new LinkedHashMap<>();
    private final StackPane inspectorHost = new StackPane();
    private final VBox editBody = new VBox(10);
    private final VBox contextBody = new VBox(10);
    private final VBox settingsBody = new VBox(10);
    private final ScrollPane editScroll = workspaceScroll(editBody, "narrative-take-editor-scroll");
    private final ScrollPane contextScroll = workspaceScroll(contextBody, "narrative-context-bank-scroll");
    private final ScrollPane settingsScroll = workspaceScroll(settingsBody, "narrative-video-settings-scroll");
    private final CollapsibleModuleSplitPane workspace;
    private final Button editMode;
    private final Button contextMode;
    private final Button settingsMode;
    private String selectedBlockId = "";
    private boolean compact;
    private InspectorMode activeMode = InspectorMode.EDIT_TAKE;

    public NarrativeVideoPanel(DocuPodcastShellViewModel viewModel) {
        this.viewModel = Objects.requireNonNull(viewModel, "viewModel");
        getStyleClass().add("narrative-video-panel");

        VBox listBody = new VBox(10,
                heading("Contenido del video"),
                hint("Una toma por parrafo narrable, en el orden original del Word."),
                takeRows);
        listBody.setPadding(new Insets(14));
        ScrollPane listScroll = workspaceScroll(listBody, "narrative-video-take-list-scroll");

        inspectorHost.getChildren().setAll(editScroll);
        workspace = new CollapsibleModuleSplitPane(
                "Editar toma",
                inspectorHost,
                "Tomas narrativas",
                listScroll,
                0.44,
                false);
        workspace.getStyleClass().add("narrative-video-workspace-split");

        editMode = modeButton(AppIcon.IMAGE, "Editar toma", InspectorMode.EDIT_TAKE);
        contextMode = modeButton(AppIcon.BUNDLE, "Banco de contexto", InspectorMode.CONTEXT_BANK);
        settingsMode = modeButton(AppIcon.SETTINGS, "Ajustes del video", InspectorMode.SETTINGS);
        VBox modeRail = new VBox(10, editMode, contextMode, settingsMode);
        modeRail.setPadding(new Insets(8, 6, 8, 6));
        modeRail.setAlignment(Pos.TOP_CENTER);
        modeRail.getStyleClass().add("narrative-video-mode-rail");

        HBox shell = new HBox(modeRail, workspace);
        HBox.setHgrow(workspace, Priority.ALWAYS);
        workspace.setMaxWidth(Double.MAX_VALUE);
        setCenter(shell);
        showMode(InspectorMode.EDIT_TAKE, false);

        widthProperty().addListener((obs, oldValue, value) -> updateResponsiveLayout(value.doubleValue()));
        viewModel.currentDocumentProperty().addListener((obs, oldValue, newValue) -> rebuild());
        viewModel.currentProjectModeProperty().addListener((obs, oldValue, newValue) -> rebuild());
        viewModel.documentMediaRevisionProperty().addListener((obs, oldValue, newValue) -> refresh());
        viewModel.narrativeVisualGenerationRunningProperty().addListener((obs, oldValue, newValue) -> refresh());
        viewModel.selectedDocumentBlockIdProperty().addListener((obs, oldValue, value) -> {
            String normalized = safe(value);
            if (rows.containsKey(normalized)) {
                select(normalized, false);
            }
        });
        rebuild();
        Platform.runLater(() -> updateResponsiveLayout(getWidth()));
    }

    private Button modeButton(AppIcon icon, String text, InspectorMode mode) {
        Button button = ActionButtonFactory.sideDockRail(icon, () -> showMode(mode, true));
        button.setText(text);
        button.setGraphicTextGap(4);
        button.setMaxWidth(Double.MAX_VALUE);
        button.getStyleClass().add("narrative-video-mode-button");
        return button;
    }

    private void showMode(InspectorMode mode, boolean reveal) {
        activeMode = mode == null ? InspectorMode.EDIT_TAKE : mode;
        inspectorHost.getChildren().setAll(switch (activeMode) {
            case EDIT_TAKE -> editScroll;
            case CONTEXT_BANK -> contextScroll;
            case SETTINGS -> settingsScroll;
        });
        editMode.pseudoClassStateChanged(SELECTED, activeMode == InspectorMode.EDIT_TAKE);
        contextMode.pseudoClassStateChanged(SELECTED, activeMode == InspectorMode.CONTEXT_BANK);
        settingsMode.pseudoClassStateChanged(SELECTED, activeMode == InspectorMode.SETTINGS);
        if (reveal) {
            workspace.primaryVisibleProperty().set(true);
            workspace.secondaryVisibleProperty().set(!compact);
        }
    }

    private void rebuild() {
        rows.clear();
        takeRows.getChildren().clear();
        selectedBlockId = "";
        if (!viewModel.narrativeVideoConfigurationAvailable()) {
            takeRows.getChildren().setAll(hint(
                    "Abre un proyecto de Video narrativo con una fuente Word/DOCX."));
            refresh();
            return;
        }
        viewModel.synchronizeNarrativeDocumentSnapshot();
        for (DocumentBlock block : viewModel.narrativeVideoParagraphs()) {
            MediaThumbnailCard card = new MediaThumbnailCard(
                    "", "Sin imagen", "Parrafo", "Bloque " + block.id(),
                    block.preview(190), "", null);
            VBox container = new VBox(card);
            TakeRow row = new TakeRow(block, card, container);
            rows.put(block.id(), row);
            takeRows.getChildren().add(container);
        }
        String selected = safe(viewModel.selectedDocumentBlockIdProperty().get());
        if (rows.containsKey(selected)) {
            selectedBlockId = selected;
        } else if (!rows.isEmpty()) {
            selectedBlockId = rows.keySet().iterator().next();
        }
        refresh();
    }

    private void refresh() {
        if (!viewModel.narrativeVideoConfigurationAvailable()) {
            editBody.getChildren().setAll(heading("Editar toma"), hint("No hay una toma narrativa disponible."));
            contextBody.getChildren().setAll(heading("Banco de contexto"), hint("No hay un proyecto narrativo activo."));
            settingsBody.getChildren().setAll(heading("Ajustes del video"), hint("No hay un proyecto narrativo activo."));
            return;
        }
        List<String> currentIds = viewModel.narrativeVideoParagraphs().stream().map(DocumentBlock::id).toList();
        if (!currentIds.equals(List.copyOf(rows.keySet()))) {
            rebuild();
            return;
        }
        NarrativeProjectLayer layer = viewModel.narrativeProjectLayer();
        for (TakeRow row : rows.values()) {
            updateRow(row, layer.takeOrDefault(row.block().id()));
        }
        refreshEdit(layer);
        refreshContext(layer);
        refreshSettings(layer.videoConfiguration());
    }

    private void updateRow(TakeRow row, NarrativeParagraphTake take) {
        String uri = viewModel.projectImageAssetUri(take.keyframeAssetId()).orElse("");
        String state = row.block().id().equals(selectedBlockId)
                ? "document-media-selected"
                : take.enabled() ? "" : "document-media-disabled";
        String status = take.keyframeReady()
                ? take.clipsReady()
                        ? "Clips: %.1f s".formatted(take.generatedDurationSeconds())
                        : take.stale() ? "Imagen lista; toma desactualizada" : "Imagen clave lista"
                : "Sin imagen clave";
        row.card().update(
                uri,
                "Sin imagen",
                "Parrafo",
                "Bloque " + row.block().id(),
                row.block().preview(170) + " - " + status,
                state,
                () -> select(row.block().id(), true),
                take.keyframeSource().name().equals("IMPORTED") ? "Importada"
                        : take.keyframeSource().name().equals("GENERATED") ? "IA local" : "",
                "",
                null,
                null,
                "",
                null);
        row.container().setOpacity(take.enabled() ? 1.0 : 0.55);
    }

    private void select(String blockId, boolean updateDocument) {
        if (!rows.containsKey(blockId)) {
            return;
        }
        selectedBlockId = blockId;
        if (updateDocument) {
            viewModel.selectDocumentBlock(blockId);
        }
        refresh();
        showMode(InspectorMode.EDIT_TAKE, true);
    }

    private void refreshEdit(NarrativeProjectLayer layer) {
        editBody.getChildren().clear();
        editBody.setPadding(new Insets(14));
        editBody.getChildren().addAll(
                heading("Editar toma"),
                hint("Estas acciones afectan unicamente al parrafo seleccionado."));
        TakeRow row = rows.get(selectedBlockId);
        if (row == null) {
            editBody.getChildren().add(hint("Selecciona un parrafo en la lista."));
            return;
        }
        NarrativeParagraphTake take = layer.takeOrDefault(selectedBlockId);
        CheckBox enabled = new CheckBox("Incluir este parrafo en narracion y video");
        enabled.setSelected(take.enabled());
        enabled.setOnAction(event ->
                viewModel.setNarrativeParagraphEnabled(selectedBlockId, enabled.isSelected()));
        Label paragraph = new Label(row.block().text());
        paragraph.setWrapText(true);
        paragraph.getStyleClass().add("workspace-subtitle");
        Button choose = ActionButtonFactory.primary("Elegir imagen clave", this::chooseKeyframe);
        Button generate = ActionButtonFactory.secondary(
                "Generar imagen clave",
                "Usa el texto del Word y el banco global con el motor local seleccionado.",
                () -> viewModel.generateNarrativeKeyframe(selectedBlockId));
        Button clips = ActionButtonFactory.primary(
                take.clipsReady() ? "Regenerar clips" : "Generar clips",
                "Encadena clips locales desde la imagen clave hasta cubrir la voz.",
                () -> viewModel.generateNarrativeClips(selectedBlockId));
        boolean running = viewModel.narrativeVisualGenerationRunningProperty().get();
        choose.setDisable(running);
        generate.setDisable(running || !take.enabled());
        clips.setDisable(running || !take.enabled() || !take.keyframeReady());
        Button cancel = ActionButtonFactory.danger(
                "Cancelar generacion",
                viewModel::cancelNarrativeVisualGeneration);
        cancel.setDisable(!running);
        Label state = new Label(takeState(take));
        state.setWrapText(true);
        state.getStyleClass().add(take.stale() ? "status-warning" : "workspace-subtitle");
        editBody.getChildren().addAll(enabled, paragraph, choose, generate, clips, cancel, state);
    }

    private void refreshContext(NarrativeProjectLayer layer) {
        contextBody.getChildren().clear();
        contextBody.setPadding(new Insets(14));
        TextArea wordContext = StudioFormControls.textInput(new TextArea(layer.normalizedDocumentText()),
                "Texto global de solo lectura procedente exclusivamente del Word importado.");
        wordContext.setEditable(false);
        wordContext.setWrapText(true);
        wordContext.setPrefRowCount(8);
        ComboBox<NarrativeContextRole> role = StudioFormControls.combo(new ComboBox<>(),
                "Define como debe usar la IA esta referencia global.");
        role.getItems().setAll(NarrativeContextRole.values());
        role.setValue(NarrativeContextRole.STYLE);
        Button importReference = ActionButtonFactory.primary(
                "Agregar referencia global",
                () -> chooseContextReference(role.getValue()));
        VBox references = new VBox(8);
        for (NarrativeContextReference reference : layer.contextReferences()) {
            references.getChildren().add(contextReferenceRow(reference));
        }
        contextBody.getChildren().addAll(
                heading("Banco de contexto"),
                hint("El Word completo aporta el contexto textual. Estas imagenes se aplican a todas las tomas."),
                labelled("Texto del Word", wordContext),
                labelled("Tipo de referencia", role),
                importReference,
                references.getChildren().isEmpty() ? hint("Sin referencias visuales globales.") : references);
    }

    private Node contextReferenceRow(NarrativeContextReference reference) {
        CheckBox enabled = new CheckBox(reference.displayName() + " - " + reference.role().name());
        enabled.setSelected(reference.enabled());
        enabled.setMaxWidth(Double.MAX_VALUE);
        enabled.setOnAction(event -> viewModel.updateNarrativeContextReference(
                new NarrativeContextReference(
                        reference.id(), reference.role(), reference.assetId(), reference.displayName(),
                        enabled.isSelected(), reference.strength(), reference.notes())));
        Button remove = ActionButtonFactory.danger("Quitar", () ->
                viewModel.removeNarrativeContextReference(reference.id()));
        HBox row = new HBox(8, enabled, remove);
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(enabled, Priority.ALWAYS);
        row.getStyleClass().add("narrative-context-reference-row");
        return row;
    }

    private void refreshSettings(NarrativeVideoConfiguration config) {
        settingsBody.getChildren().clear();
        settingsBody.setPadding(new Insets(14));
        ComboBox<VisualGenerationProfile> imageProfile = StudioFormControls.combo(new ComboBox<>(),
                "Perfil local usado para producir imagenes clave.");
        imageProfile.getItems().setAll(VisualGenerationProfile.values());
        imageProfile.setValue(VisualGenerationProfile.from(config.imageProfile()));
        ComboBox<VisualClipGenerationProfile> videoProfile = StudioFormControls.combo(new ComboBox<>(),
                "Perfil local image-to-video para producir los clips.");
        videoProfile.getItems().setAll(VisualClipGenerationProfile.values());
        videoProfile.setValue(VisualClipGenerationProfile.from(config.videoProfile()));
        ComboBox<String> memory = StudioFormControls.combo(new ComboBox<>(),
                "La memoria baja puede descargar capas a RAM sin cambiar el dispositivo seleccionado.");
        memory.getItems().setAll("SAFE", "BALANCED", "QUALITY");
        memory.setValue(config.memoryMode());
        Spinner<Integer> fps = StudioFormControls.spinner(
                new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(12, 60, config.framesPerSecond())),
                "Fotogramas por segundo del video final.");
        Spinner<Double> clipSeconds = StudioFormControls.spinner(
                new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(
                        1.0, 10.0, config.maxClipDurationSeconds(), 0.5)),
                "Duracion maxima de cada clip antes de encadenar el siguiente.");
        Button save = ActionButtonFactory.primary("Guardar ajustes", () ->
                viewModel.updateNarrativeVideoConfiguration(new NarrativeVideoConfiguration(
                        720,
                        1280,
                        fps.getValue(),
                        clipSeconds.getValue(),
                        imageProfile.getValue().name(),
                        videoProfile.getValue().name(),
                        memory.getValue(),
                        config.customWorkflowPath())));
        Button generateAll = ActionButtonFactory.secondary(
                "Generar tomas pendientes",
                "Genera imagen y clips para los parrafos habilitados que esten pendientes.",
                viewModel::generatePendingNarrativeTakes);
        boolean running = viewModel.narrativeVisualGenerationRunningProperty().get();
        save.setDisable(running);
        generateAll.setDisable(running);
        Button cancel = ActionButtonFactory.danger(
                "Cancelar generacion",
                viewModel::cancelNarrativeVisualGeneration);
        cancel.setDisable(!running);
        settingsBody.getChildren().addAll(
                heading("Ajustes del video"),
                hint("Formato vertical 9:16, salida minima 720x1280 y dispositivo exacto de Configuracion."),
                labelled("Resolucion", new Label(config.width() + " x " + config.height())),
                labelled("FPS", fps),
                labelled("Duracion maxima de clip", clipSeconds),
                labelled("Perfil de imagen", imageProfile),
                labelled("Perfil de video", videoProfile),
                labelled("Memoria", memory),
                save,
                generateAll,
                cancel);
    }

    private void chooseKeyframe() {
        FileChooser chooser = imageChooser("Elegir imagen clave");
        File selected = chooser.showOpenDialog(getScene() == null ? null : getScene().getWindow());
        if (selected == null) {
            return;
        }
        try {
            viewModel.importNarrativeKeyframe(selectedBlockId, selected.toPath());
        } catch (IOException | RuntimeException ex) {
            viewModel.reportUserVisibleError("No se pudo importar la imagen clave: " + message(ex));
        }
    }

    private void chooseContextReference(NarrativeContextRole role) {
        FileChooser chooser = imageChooser("Agregar referencia global");
        File selected = chooser.showOpenDialog(getScene() == null ? null : getScene().getWindow());
        if (selected == null) {
            return;
        }
        try {
            viewModel.importNarrativeContextReference(selected.toPath(),
                    role == null ? NarrativeContextRole.STYLE : role);
        } catch (IOException | RuntimeException ex) {
            viewModel.reportUserVisibleError("No se pudo importar la referencia: " + message(ex));
        }
    }

    private void updateResponsiveLayout(double width) {
        if (width <= 0) {
            return;
        }
        boolean nextCompact = width < COMPACT_WIDTH;
        if (nextCompact == compact) {
            return;
        }
        compact = nextCompact;
        if (compact) {
            workspace.secondaryVisibleProperty().set(!workspace.primaryVisibleProperty().get());
        } else {
            workspace.secondaryVisibleProperty().set(true);
        }
    }

    private static ScrollPane workspaceScroll(Node content, String styleClass) {
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setMinWidth(0);
        scroll.getStyleClass().addAll("narrative-video-scroll", styleClass);
        return scroll;
    }

    private static VBox labelled(String label, Node control) {
        Label title = new Label(label);
        title.getStyleClass().add("workspace-field-label");
        VBox box = new VBox(5, title, control);
        box.setFillWidth(true);
        return box;
    }

    private static Label heading(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("workspace-section-title");
        return label;
    }

    private static Label hint(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.getStyleClass().add("workspace-subtitle");
        return label;
    }

    private static String takeState(NarrativeParagraphTake take) {
        if (!take.enabled()) {
            return "Excluido: no se narrara ni se renderizara.";
        }
        if (!take.keyframeReady()) {
            return "Pendiente de imagen clave.";
        }
        if (take.clips().isEmpty()) {
            return "Imagen clave lista. Pendiente de clips.";
        }
        if (take.stale()) {
            return "Resultados conservados, pero desactualizados por cambios de contexto o configuracion.";
        }
        return "Toma lista: %.1f segundos de clips generados.".formatted(take.generatedDurationSeconds());
    }

    private static FileChooser imageChooser(String title) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().setAll(
                new FileChooser.ExtensionFilter("Imagenes compatibles", "*.png", "*.jpg", "*.jpeg", "*.webp", "*.bmp"),
                new FileChooser.ExtensionFilter("PNG", "*.png"));
        return chooser;
    }

    private static String message(Throwable error) {
        return error == null || error.getMessage() == null || error.getMessage().isBlank()
                ? "Error inesperado."
                : error.getMessage();
    }

    private static String safe(String value) {
        return value == null ? "" : value.strip();
    }

    private record TakeRow(DocumentBlock block, MediaThumbnailCard card, VBox container) {
    }

    private enum InspectorMode {
        EDIT_TAKE,
        CONTEXT_BANK,
        SETTINGS
    }
}

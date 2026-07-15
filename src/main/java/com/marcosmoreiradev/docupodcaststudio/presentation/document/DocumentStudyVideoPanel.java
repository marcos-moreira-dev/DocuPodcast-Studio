package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import com.marcosmoreiradev.docupodcaststudio.application.documentstudy.DocumentStudyVideoContentResolver;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentMascotPosition;
import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentParagraphVisualAssignment;
import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentStudyMusicTrack;
import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentStudyClosingSlide;
import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentStudyVideoConfiguration;
import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentTableSlideConfiguration;
import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentVisualSource;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.ActionButtonFactory;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.CollapsibleModuleSplitPane;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.MediaThumbnailCard;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioFormControls;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.DocuPodcastShellViewModel;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
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

/** Configures paragraph and table slides for a DOCX documentary video. */
public final class DocumentStudyVideoPanel extends BorderPane {
    private final DocuPodcastShellViewModel viewModel;
    private final DocumentStudyVideoContentResolver resolver = new DocumentStudyVideoContentResolver();
    private final TextField title = StudioFormControls.textInput(new TextField(),
            "Titulo opcional mostrado en la zona superior de las diapositivas.");
    private final Spinner<Double> defaultTableDuration = StudioFormControls.spinner(
            new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(2.0, 60.0, 6.0, 1.0)),
            "Segundos durante los que se muestra cada tabla sin narracion.");
    private final VBox contentRows = new VBox(9);
    private final VBox selectedEditor = new VBox(10);
    private final VBox musicRows = new VBox(8);
    private final Map<String, ContentRow> rowsByBlock = new LinkedHashMap<>();
    private List<DocumentStudyVideoContentResolver.Item> contentItems = List.of();
    private ReadableDocument renderedDocument;
    private String selectedBlockId = "";
    private boolean syncingControls;

    public DocumentStudyVideoPanel(DocuPodcastShellViewModel viewModel) {
        this.viewModel = Objects.requireNonNull(viewModel, "viewModel");
        getStyleClass().add("document-study-video-panel");

        title.setPromptText("Titulo del video (opcional)");
        title.setMaxWidth(Double.MAX_VALUE);
        title.setOnAction(event -> commitTitle());
        title.focusedProperty().addListener((obs, oldValue, focused) -> {
            if (!Boolean.TRUE.equals(focused)) commitTitle();
        });
        defaultTableDuration.setEditable(true);
        defaultTableDuration.valueProperty().addListener((obs, oldValue, value) -> {
            if (!syncingControls && value != null) {
                saveConfiguration(configuration().withDefaultTableDuration(value));
            }
        });

        Label contentHeading = sectionTitle("Contenido del video");
        Label contentHint = hint("Una fila por parrafo y por tabla, en el orden original del Word.");
        Label selectedHeading = sectionTitle("Contenido seleccionado");
        Label musicHeading = sectionTitle("Musica de fondo");
        Label musicHint = hint("Las pistas se reproducen en secuencia y vuelven a empezar si el video continua.");
        Button addMusic = ActionButtonFactory.secondary(
                "Agregar musica",
                "Importar una o varias pistas dentro de la carpeta del proyecto.",
                this::chooseMusic);
        Button addClosingSlide = ActionButtonFactory.primary(
                "Agregar nuevo parrafo final",
                "Anadir una diapositiva silenciosa con titulo, imagen y duracion propios.",
                this::addClosingSlide);

        GridPane general = new GridPane();
        general.setHgap(8);
        general.setVgap(7);
        general.add(fieldLabel("Titulo del video"), 0, 0);
        general.add(title, 0, 1);
        general.add(fieldLabel("Duracion de tablas"), 0, 2);
        general.add(defaultTableDuration, 0, 3);
        GridPane.setHgrow(title, Priority.ALWAYS);
        GridPane.setHgrow(defaultTableDuration, Priority.ALWAYS);

        VBox inspectorBody = new VBox(12,
                general,
                new Separator(),
                selectedHeading,
                selectedEditor,
                new Separator(),
                musicHeading,
                musicHint,
                addMusic,
                musicRows);
        inspectorBody.setPadding(new Insets(14));
        inspectorBody.setFillWidth(true);

        VBox contentBody = new VBox(10, contentHeading, contentHint, addClosingSlide, contentRows);
        contentBody.setPadding(new Insets(14));
        contentBody.setFillWidth(true);

        ScrollPane inspectorScroll = workspaceScroll(inspectorBody, "document-study-video-inspector-scroll");
        ScrollPane contentScroll = workspaceScroll(contentBody, "document-study-video-content-scroll");
        CollapsibleModuleSplitPane workspace = new CollapsibleModuleSplitPane(
                "Acciones del contenido",
                inspectorScroll,
                "Contenido del video",
                contentScroll,
                0.43,
                false);
        workspace.getStyleClass().add("document-study-video-workspace-split");
        setCenter(workspace);

        viewModel.currentDocumentProperty().addListener((obs, oldValue, newValue) -> rebuildForDocument(newValue));
        viewModel.currentProjectModeProperty().addListener((obs, oldValue, newValue) ->
                rebuildForDocument(viewModel.currentDocumentProperty().get()));
        viewModel.documentMediaRevisionProperty().addListener((obs, oldValue, newValue) -> refreshConfiguration());
        viewModel.selectedDocumentBlockIdProperty().addListener((obs, oldValue, newValue) -> {
            String normalized = normalize(newValue);
            if (rowsByBlock.containsKey(normalized)) selectRow(normalized, false);
        });
        rebuildForDocument(viewModel.currentDocumentProperty().get());
    }

    private static ScrollPane workspaceScroll(VBox content, String styleClass) {
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setMinWidth(0);
        scroll.getStyleClass().addAll("document-study-video-scroll", styleClass);
        return scroll;
    }

    private void rebuildForDocument(ReadableDocument document) {
        renderedDocument = document;
        rowsByBlock.clear();
        contentRows.getChildren().clear();
        contentItems = List.of();
        if (!viewModel.documentaryVideoConfigurationAvailable() || document == null) {
            selectedBlockId = "";
            selectedEditor.getChildren().setAll(hint("Abre un proyecto de estudio documental con una fuente Word/DOCX."));
            refreshConfigurationControls();
            refreshMusicRows();
            return;
        }
        contentItems = resolver.resolve(document, configuration()).stream()
                .filter(item -> item.kind() != DocumentStudyVideoContentResolver.Kind.COVER)
                .toList();
        for (DocumentStudyVideoContentResolver.Item item : contentItems) {
            ContentRow row = createRow(item);
            rowsByBlock.put(item.block().id(), row);
            contentRows.getChildren().add(row.container());
        }
        String documentSelection = normalize(viewModel.selectedDocumentBlockIdProperty().get());
        if (rowsByBlock.containsKey(documentSelection)) selectedBlockId = documentSelection;
        else if (!rowsByBlock.containsKey(selectedBlockId)) selectedBlockId = "";
        refreshConfiguration();
    }

    private ContentRow createRow(DocumentStudyVideoContentResolver.Item item) {
        MediaThumbnailCard card = new MediaThumbnailCard(
                "", item.kind() == DocumentStudyVideoContentResolver.Kind.TABLE ? "Tabla" : "Sin imagen",
                "", "", "", "", null);
        VBox container = new VBox(card);
        container.setFillWidth(true);
        ContentRow row = new ContentRow(item, card, container);
        installContentMenu(row);
        return row;
    }

    private void refreshConfiguration() {
        if (renderedDocument != viewModel.currentDocumentProperty().get()) {
            rebuildForDocument(viewModel.currentDocumentProperty().get());
            return;
        }
        if (renderedDocument != null && viewModel.documentaryVideoConfigurationAvailable()) {
            List<String> resolvedIds = resolver.resolve(renderedDocument, configuration()).stream()
                    .filter(item -> item.kind() != DocumentStudyVideoContentResolver.Kind.COVER)
                    .map(item -> item.block().id())
                    .toList();
            if (!resolvedIds.equals(List.copyOf(rowsByBlock.keySet()))) {
                rebuildForDocument(renderedDocument);
                return;
            }
        }
        refreshConfigurationControls();
        for (ContentRow row : rowsByBlock.values()) updateRow(row);
        refreshSelectedEditor();
        refreshMusicRows();
    }

    private void refreshConfigurationControls() {
        DocumentStudyVideoConfiguration configuration = configuration();
        syncingControls = true;
        try {
            if (!Objects.equals(title.getText(), configuration.videoTitle())) title.setText(configuration.videoTitle());
            defaultTableDuration.getValueFactory().setValue(configuration.defaultTableDurationSeconds());
        } finally {
            syncingControls = false;
        }
    }

    private void updateRow(ContentRow row) {
        DocumentBlock block = row.item().block();
        boolean selected = block.id().equals(selectedBlockId);
        boolean enabled = configuration().blockEnabled(block.id());
        String stateClass = rowStateClass(selected, enabled);
        if (row.item().kind() == DocumentStudyVideoContentResolver.Kind.TABLE) {
            int rows = integer(block.metadata().get("table.rowCount"));
            int columns = integer(block.metadata().get("table.columnCount"));
            double duration = configuration().tableDuration(block.id());
            row.card().update("", "Tabla", enabled ? "Tabla" : "Tabla deshabilitada", "Bloque " + block.id(),
                    rows + " filas x " + columns + " columnas - " + seconds(duration),
                    stateClass, () -> selectRow(block.id(), true),
                    enabled ? "Tabla" : "Deshabilitado", "", null, null, "", null);
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
        String imageUri = viewModel.resolveCurrentProjectAsset(assetId)
                .map(path -> path.toUri().toString()).orElse("");
        String badge = switch (visual.activeSource()) {
            case IMPORTED -> "Importada";
            case DRAWN -> "Dibujo";
            case NONE -> "";
        };
        String mascot = visual.mascotAssetId().isBlank() ? "" : " - mascota/logo asignado";
        String visibleBadge = enabled ? badge : (badge.isBlank() ? "Deshabilitado" : badge + " / Deshabilitado");
        row.card().update(imageUri, "Sin imagen", enabled ? "Parrafo" : "Parrafo deshabilitado",
                "Bloque " + block.id(), compactPreview(block.text()) + mascot, stateClass,
                () -> selectRow(block.id(), true), visibleBadge, "", null, null, "", null);
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
        if (synchronizeDocument && sourceBlock
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
        } else if (row.item().kind() == DocumentStudyVideoContentResolver.Kind.CLOSING) {
            buildClosingEditor(configuration().closingSlide(row.item().block().id()).orElseThrow());
        } else {
            buildParagraphEditor(row.item().block());
        }
    }

    private void buildParagraphEditor(DocumentBlock block) {
        DocumentParagraphVisualAssignment visual = assignment(block);
        Label text = new Label(block.text());
        text.setWrapText(true);
        text.getStyleClass().add("document-study-video-selected-text");

        Button chooseImage = ActionButtonFactory.secondary("Elegir imagen", () -> chooseParagraphImage(block));
        Button draw = ActionButtonFactory.secondary("Dibujar/editar", () -> drawParagraphImage(block));
        HBox imageActions = new HBox(8, chooseImage, draw);
        HBox.setHgrow(chooseImage, Priority.ALWAYS);
        HBox.setHgrow(draw, Priority.ALWAYS);
        chooseImage.setMaxWidth(Double.MAX_VALUE);
        draw.setMaxWidth(Double.MAX_VALUE);

        ComboBox<DocumentVisualSource> source = StudioFormControls.combo(
                new ComboBox<>(FXCollections.observableArrayList(availableSources(visual))),
                "Elegir cual imagen se usara como principal sin eliminar la otra.");
        source.setConverter(sourceConverter());
        source.setValue(visual.activeSource());
        source.setMaxWidth(Double.MAX_VALUE);
        source.setOnAction(event -> {
            DocumentVisualSource value = source.getValue();
            if (value != null) saveParagraph(assignment(block).withActiveSource(value));
        });

        Button chooseMascot = ActionButtonFactory.secondary("Elegir mascota/logo", () -> chooseMascot(block));
        Button clearMascot = ActionButtonFactory.danger("Quitar mascota/logo", () ->
                saveParagraph(assignment(block).withMascot("", assignment(block).mascotPosition())));
        clearMascot.setDisable(visual.mascotAssetId().isBlank());

        ComboBox<DocumentMascotPosition> position = StudioFormControls.combo(
                new ComboBox<>(FXCollections.observableArrayList(DocumentMascotPosition.values())),
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
                imageActions,
                fieldLabel("Imagen principal activa"), source,
                chooseMascot,
                fieldLabel("Posicion de mascota/logo"), position,
                clearMascot,
                clearImage);
    }

    private void buildTableEditor(DocumentBlock block) {
        int rowCount = integer(block.metadata().get("table.rowCount"));
        int columnCount = integer(block.metadata().get("table.columnCount"));
        Label summary = new Label("Tabla " + rowCount + " x " + columnCount + " - no se narra.");
        summary.setWrapText(true);
        GridPane preview = tablePreview(block, rowCount, columnCount);
        Spinner<Double> duration = StudioFormControls.spinner(
                new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(
                        2.0, 60.0, configuration().tableDuration(block.id()), 1.0)),
                "Duracion especifica de esta tabla.");
        duration.setEditable(true);
        Button save = ActionButtonFactory.primary("Guardar duracion", () ->
                saveConfiguration(configuration().withTable(
                        new DocumentTableSlideConfiguration(block.id(), duration.getValue()))));
        Button restore = ActionButtonFactory.secondary("Usar duracion predeterminada", () ->
                saveConfiguration(configuration().withoutTable(block.id())));
        selectedEditor.getChildren().addAll(summary, preview, fieldLabel("Duracion de esta tabla"), duration, save, restore);
    }

    private void buildClosingEditor(DocumentStudyClosingSlide slide) {
        Label explanation = hint("Esta diapositiva no se narra. La musica de fondo continua durante toda su duracion.");
        TextField closingTitle = StudioFormControls.textInput(new TextField(slide.title()),
                "Titulo opcional de despedida o creditos.");
        closingTitle.setPromptText("Titulo opcional");
        closingTitle.setMaxWidth(Double.MAX_VALUE);
        Spinner<Double> duration = StudioFormControls.spinner(
                new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(
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
        rebuildForDocument(viewModel.currentDocumentProperty().get());
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
        rebuildForDocument(viewModel.currentDocumentProperty().get());
    }

    private void drawParagraphImage(DocumentBlock block) {
        DocumentParagraphVisualAssignment current = assignment(block);
        Path state = viewModel.resolveCurrentProjectRelativePath(current.drawnStateRelativePath()).orElse(null);
        DocumentParagraphSketchDialog dialog = new DocumentParagraphSketchDialog(
                getScene() == null ? null : getScene().getWindow(), block.text(), state);
        Optional<DocumentParagraphSketchDialog.Result> result = dialog.showAndWait();
        if (result.isEmpty()) return;
        try {
            var asset = viewModel.saveDocumentaryDrawing(block.id(), result.get().png(), result.get().inkStateJson());
            saveParagraph(assignment(block).withDrawnImage(asset.asset().id(), asset.stateRelativePath()));
        } catch (Exception ex) {
            showError("No se pudo guardar el dibujo", ex);
        } finally {
            try { Files.deleteIfExists(result.get().png()); } catch (IOException ignored) { }
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
        imageNext.setOnAction(event -> copyImageToNext(block));
        MenuItem imageAsMascot = new MenuItem("Establecer imagen como mascota/logo");
        imageAsMascot.setOnAction(event -> useImageAsMascot(block));
        MenuItem mascotNext = new MenuItem("Usar la misma mascota en el parrafo siguiente");
        mascotNext.setOnAction(event -> copyMascotToNext(block));
        MenuItem mascotRemaining = new MenuItem("Aplicar mascota a todos los parrafos restantes");
        mascotRemaining.setOnAction(event -> copyMascotToRemaining(block));
        MenuItem removeClosing = new MenuItem("Eliminar esta diapositiva");
        removeClosing.setOnAction(event -> removeClosingSlide(block.id()));
        ContextMenu menu = switch (row.item().kind()) {
            case PARAGRAPH -> new ContextMenu(availability, imageNext, imageAsMascot, mascotNext, mascotRemaining);
            case CLOSING -> new ContextMenu(availability, removeClosing);
            default -> new ContextMenu(availability);
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
            DocumentParagraphVisualAssignment destination = assignment(target);
            destination = switch (source.activeSource()) {
                case IMPORTED -> destination.withImportedImage(source.importedImageAssetId());
                case DRAWN -> destination.withDrawnImage(source.drawnImageAssetId(), source.drawnStateRelativePath());
                case NONE -> destination;
            };
            saveParagraph(destination);
        });
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

    private void chooseMusic() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Agregar musica de fondo");
        chooser.getExtensionFilters().setAll(
                new FileChooser.ExtensionFilter("Audio compatible", "*.wav", "*.mp3", "*.m4a", "*.flac", "*.ogg"),
                new FileChooser.ExtensionFilter("Todos los archivos", "*.*"));
        List<File> selected = chooser.showOpenMultipleDialog(getScene() == null ? null : getScene().getWindow());
        if (selected == null || selected.isEmpty()) return;
        ArrayList<DocumentStudyMusicTrack> tracks = new ArrayList<>(configuration().musicTracks());
        try {
            for (File file : selected) tracks.add(viewModel.importDocumentaryMusic(file.toPath()));
            saveConfiguration(configuration().withMusicTracks(tracks));
        } catch (Exception ex) {
            showError("No se pudo importar la musica", ex);
        }
    }

    private void refreshMusicRows() {
        musicRows.getChildren().clear();
        List<DocumentStudyMusicTrack> tracks = configuration().musicTracks();
        if (tracks.isEmpty()) {
            musicRows.getChildren().add(hint("Sin musica de fondo."));
            return;
        }
        for (int index = 0; index < tracks.size(); index++) {
            DocumentStudyMusicTrack track = tracks.get(index);
            musicRows.getChildren().add(musicRow(track, index, tracks.size()));
        }
    }

    private Node musicRow(DocumentStudyMusicTrack track, int index, int size) {
        String displayName = viewModel.currentProject()
                .flatMap(project -> project.assets().byId(track.assetId()))
                .map(ProjectAssetReference::displayName).orElse(track.assetId());
        Label name = new Label((index + 1) + ". " + compactMusicName(displayName));
        name.setWrapText(true);
        name.setMaxHeight(42);
        Tooltip.install(name, new Tooltip(displayName));
        name.getStyleClass().add("document-study-video-music-name");
        Label duration = hint("Duracion: " + seconds(track.durationSeconds()));
        Slider volume = StudioFormControls.slider(new Slider(0.0, 1.0, track.volume()), "Volumen de esta pista.");
        volume.setBlockIncrement(0.05);
        Label value = new Label(percent(track.volume()));
        volume.valueProperty().addListener((obs, oldValue, newValue) -> value.setText(percent(newValue.doubleValue())));
        volume.setOnMouseReleased(event -> updateMusicVolume(track.id(), volume.getValue()));
        volume.setOnKeyReleased(event -> updateMusicVolume(track.id(), volume.getValue()));
        Label volumeLabel = new Label("Volumen");
        volumeLabel.getStyleClass().add("document-study-video-music-volume-label");
        Region volumeSpacer = new Region();
        HBox.setHgrow(volumeSpacer, Priority.ALWAYS);
        HBox volumeHeading = new HBox(8, volumeLabel, volumeSpacer, value);
        volumeHeading.setAlignment(Pos.CENTER_LEFT);
        volume.setMaxWidth(Double.MAX_VALUE);
        VBox volumeGroup = new VBox(4, volumeHeading, volume);

        Button up = ActionButtonFactory.secondary("Subir", () -> moveMusic(index, -1));
        Button down = ActionButtonFactory.secondary("Bajar", () -> moveMusic(index, 1));
        Button remove = ActionButtonFactory.danger("Quitar", () -> removeMusic(track.id()));
        up.setDisable(index == 0);
        down.setDisable(index >= size - 1);
        up.setMaxWidth(Double.MAX_VALUE);
        down.setMaxWidth(Double.MAX_VALUE);
        remove.setMaxWidth(Double.MAX_VALUE);
        GridPane actions = new GridPane();
        actions.setHgap(6);
        actions.setVgap(6);
        actions.add(up, 0, 0);
        actions.add(down, 1, 0);
        actions.add(remove, 0, 1, 2, 1);
        GridPane.setHgrow(up, Priority.ALWAYS);
        GridPane.setHgrow(down, Priority.ALWAYS);
        GridPane.setHgrow(remove, Priority.ALWAYS);
        VBox row = new VBox(6, name, duration, volumeGroup, actions);
        row.getStyleClass().add("document-study-video-music-row");
        return row;
    }

    private void updateMusicVolume(String trackId, double volume) {
        List<DocumentStudyMusicTrack> tracks = configuration().musicTracks().stream()
                .map(track -> track.id().equals(trackId) ? track.withVolume(volume) : track).toList();
        saveConfiguration(configuration().withMusicTracks(tracks));
    }

    private void moveMusic(int index, int delta) {
        ArrayList<DocumentStudyMusicTrack> tracks = new ArrayList<>(configuration().musicTracks());
        int target = index + delta;
        if (index < 0 || index >= tracks.size() || target < 0 || target >= tracks.size()) return;
        DocumentStudyMusicTrack value = tracks.remove(index);
        tracks.add(target, value);
        saveConfiguration(configuration().withMusicTracks(tracks));
    }

    private void removeMusic(String trackId) {
        List<DocumentStudyMusicTrack> tracks = configuration().musicTracks().stream()
                .filter(track -> !track.id().equals(trackId)).toList();
        saveConfiguration(configuration().withMusicTracks(tracks));
    }

    private DocumentParagraphVisualAssignment assignment(DocumentBlock block) {
        return configuration().paragraph(block.id()).orElseGet(() -> new DocumentParagraphVisualAssignment(
                block.id(), fingerprint(block.text()), "", "", "", DocumentVisualSource.NONE,
                "", DocumentMascotPosition.BOTTOM_RIGHT));
    }

    private void saveParagraph(DocumentParagraphVisualAssignment assignment) {
        saveConfiguration(configuration().withParagraph(assignment));
    }

    private void saveConfiguration(DocumentStudyVideoConfiguration configuration) {
        viewModel.updateDocumentaryVideoConfiguration(configuration);
    }

    private DocumentStudyVideoConfiguration configuration() {
        return viewModel.documentaryVideoConfiguration();
    }

    private void commitTitle() {
        if (syncingControls) return;
        String value = normalize(title.getText());
        if (!value.equals(configuration().videoTitle())) saveConfiguration(configuration().withTitle(value));
    }

    private static List<DocumentVisualSource> availableSources(DocumentParagraphVisualAssignment visual) {
        ArrayList<DocumentVisualSource> result = new ArrayList<>();
        result.add(DocumentVisualSource.NONE);
        if (!visual.importedImageAssetId().isBlank()) result.add(DocumentVisualSource.IMPORTED);
        if (!visual.drawnImageAssetId().isBlank()) result.add(DocumentVisualSource.DRAWN);
        return result;
    }

    private static StringConverter<DocumentVisualSource> sourceConverter() {
        return new StringConverter<>() {
            @Override public String toString(DocumentVisualSource source) {
                if (source == null) return "Sin imagen";
                return switch (source) {
                    case NONE -> "Sin imagen";
                    case IMPORTED -> "Imagen importada";
                    case DRAWN -> "Dibujo";
                };
            }
            @Override public DocumentVisualSource fromString(String value) { return DocumentVisualSource.NONE; }
        };
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
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().setAll(
                new FileChooser.ExtensionFilter("Imagenes compatibles", "*.png", "*.jpg", "*.jpeg", "*.webp", "*.gif"),
                new FileChooser.ExtensionFilter("PNG recomendado", "*.png"));
        return chooser;
    }

    private void showError(String header, Exception ex) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.initOwner(getScene() == null ? null : getScene().getWindow());
        alert.setTitle("Video documental");
        alert.setHeaderText(header);
        alert.setContentText(rootMessage(ex));
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

    private static String percent(double value) {
        return Math.round(Math.max(0, Math.min(1, value)) * 100) + "%";
    }

    private static String compactPreview(String text) {
        String normalized = normalize(text);
        int limit = 190;
        if (normalized.length() <= limit) return normalized;
        return normalized.substring(0, limit).stripTrailing() + "...";
    }

    private static String compactMusicName(String text) {
        String normalized = normalize(text);
        int limit = 42;
        if (normalized.length() <= limit) return normalized;
        return normalized.substring(0, limit - 3).stripTrailing() + "...";
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
}

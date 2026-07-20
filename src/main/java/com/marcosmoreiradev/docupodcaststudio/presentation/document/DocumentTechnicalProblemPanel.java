package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import com.marcosmoreiradev.docupodcaststudio.ink.DrawingFeatureCatalog;
import com.marcosmoreiradev.docupodcaststudio.application.document.DocumentOutlineEntry;
import com.marcosmoreiradev.docupodcaststudio.application.document.DocumentOutlineProjection;
import com.marcosmoreiradev.docupodcaststudio.application.documentstudy.StudyProblemDetail;
import com.marcosmoreiradev.docupodcaststudio.application.documentstudy.StudyProblemFilter;
import com.marcosmoreiradev.docupodcaststudio.application.documentstudy.StudyProblemListItem;
import com.marcosmoreiradev.docupodcaststudio.application.documentstudy.StudyProblemSourceProjection;
import com.marcosmoreiradev.docupodcaststudio.application.documentstudy.StudyProblemStatusFilter;
import com.marcosmoreiradev.docupodcaststudio.application.documentstudy.StudyProblemsProjection;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentFormat;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.ActionButtonFactory;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioFormControls;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.DocuPodcastShellViewModel;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;

import java.io.File;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/** SideDock module for assembling, searching and managing technical problems from document blocks. */
public final class DocumentTechnicalProblemPanel extends VBox {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    private final DocuPodcastShellViewModel viewModel;
    private final ObservableSet<String> selectedBlockIds;
    private final ObservableList<PdfRegionCaptureDraft> selectedPdfRegions;
    private final Supplier<List<DocumentBlock>> selectedBlocksSupplier;
    private final Supplier<List<PdfRegionCaptureDraft>> selectedPdfRegionsSupplier;
    private final Label selectionHint = new Label();
    private final Label savedDetail = new Label();
    private final TextField search = new TextField();
    private final TextField pageFrom = new TextField();
    private final TextField pageTo = new TextField();
    private final TextField blockQuery = new TextField();
    private final ComboBox<ChapterOption> chapterFilter = new ComboBox<>();
    private final ComboBox<StudyProblemStatusFilter> statusFilter =
            new ComboBox<>(FXCollections.observableArrayList(StudyProblemStatusFilter.values()));
    private final ComboBox<StudyProblemSourceProjection> sourceSelector = new ComboBox<>();
    private final Label status = new Label();
    private final Label count = new Label();
    private final Label savedStatus = new Label();
    private final ListView<StudyProblemListItem> savedProblems = new ListView<>();
    private final Button editSaved = ActionButtonFactory.secondary(
            "Abrir / editar solucion",
            "Abrir el problema guardado para editar solucion textual, notas o lienzo.",
            null);
    private final Button exportPng = ActionButtonFactory.secondary(
            "Exportar PNG",
            "Exportar la solucion manuscrita del problema seleccionado.",
            null);
    private final Button exportText = ActionButtonFactory.secondary(
            "Exportar texto",
            "Exportar la solucion textual del problema seleccionado.",
            null);
    private final Button deleteSaved = ActionButtonFactory.danger(
            "Eliminar",
            "Eliminar el problema guardado y sus assets de estudio.",
            null);
    private final Button goToSource = ActionButtonFactory.secondary(
            "Ir a fuente",
            "Seleccionar el bloque fuente del problema en el lector.",
            null);
    private final Button exportAllPng = ActionButtonFactory.secondary(
            "Renderizar y exportar todos...",
            "Exportar en una carpeta todas las soluciones PNG guardadas y generar un manifiesto.",
            null);
    private final Button exportAllPdf = ActionButtonFactory.secondary(
            "Exportar ejercicios en PDF...",
            "Crear un PDF con una pagina por cada solucion PNG guardada.",
            null);
    private final Button createPdfFromImages = ActionButtonFactory.secondary(
            "Crear fuente PDF desde carpeta de imagenes...",
            "Convertir una carpeta de imagenes en un PDF fuente para estudiar y capturar regiones.",
            null);
    private StudyProblemsProjection projection = StudyProblemsProjection.empty();

    public DocumentTechnicalProblemPanel(DocuPodcastShellViewModel viewModel,
                                         ObservableSet<String> selectedBlockIds,
                                         ObservableList<PdfRegionCaptureDraft> selectedPdfRegions,
                                         Supplier<List<DocumentBlock>> selectedBlocksSupplier,
                                         Supplier<List<PdfRegionCaptureDraft>> selectedPdfRegionsSupplier,
                                         Runnable clearSelection,
                                         Runnable generateProblem) {
        this.viewModel = Objects.requireNonNull(viewModel, "viewModel");
        this.selectedBlockIds = selectedBlockIds;
        this.selectedPdfRegions = selectedPdfRegions;
        this.selectedBlocksSupplier = selectedBlocksSupplier;
        this.selectedPdfRegionsSupplier = selectedPdfRegionsSupplier;
        getStyleClass().add("document-technical-problem-panel");
        setPadding(new Insets(14));
        setSpacing(12);

        Label title = new Label("Problema tecnico");
        title.getStyleClass().add("document-technical-problem-title");
        status.getStyleClass().add("document-technical-problem-status");
        count.getStyleClass().add("document-technical-problem-count");
        savedStatus.getStyleClass().add("document-technical-problem-count");

        configureDetailLabels();
        configureFilters();
        configureSavedProblemsList();
        configureSourceNavigation();
        configureSavedActions();

        Button toggle = ActionButtonFactory.primary(
                "Seleccionar fragmentos para asociar a problema",
                "Activar la seleccion de fuentes del documento para construir el enunciado.",
                viewModel::toggleTechnicalProblemPreparation);
        toggle.textProperty().bind(Bindings.createStringBinding(() -> {
            boolean pdf = currentDocumentIsPdf();
            boolean active = viewModel.technicalProblemPreparationActiveProperty().get();
            if (active) {
                return pdf ? "Dejar de seleccionar capturas PDF" : "Dejar de seleccionar fragmentos";
            }
            return pdf ? "Seleccionar capturas del PDF para asociar a problema" : "Seleccionar fragmentos para asociar a problema";
        }, viewModel.technicalProblemPreparationActiveProperty(), viewModel.currentDocumentProperty()));
        fullWidth(toggle);

        Button clear = ActionButtonFactory.secondary(
                "Limpiar seleccion",
                "Quitar los bloques marcados para el nuevo problema.",
                clearSelection);
        fullWidth(clear);

        Button generate = ActionButtonFactory.primary(
                "Generar problema",
                "Crear un problema tecnico con las fuentes seleccionadas.",
                generateProblem);
        generate.disableProperty().bind(Bindings.createBooleanBinding(
                () -> currentDocumentIsPdf()
                        ? selectedPdfRegions == null || selectedPdfRegions.isEmpty()
                        : selectedBlockIds == null || selectedBlockIds.isEmpty(),
                viewModel.currentDocumentProperty(),
                selectedPdfRegions,
                selectedBlockIds));
        fullWidth(generate);

        VBox actions = new VBox(8, clear, generate);
        actions.getStyleClass().add("document-technical-problem-actions");

        getChildren().addAll(title, section("Nuevo problema"), status, count, selectionHint, toggle, actions,
                sourcePdfActions(), section("Problemas guardados"), savedFilters(), savedStatus, savedProblems, savedDetail,
                sourceNavigation(), savedActions());

        selectedBlockIds.addListener((SetChangeListener<String>) change -> refreshNewProblem());
        selectedPdfRegions.addListener((ListChangeListener<PdfRegionCaptureDraft>) change -> refreshNewProblem());
        viewModel.technicalProblemPreparationActiveProperty().addListener((obs, oldValue, newValue) -> refreshNewProblem());
        viewModel.documentMediaRevisionProperty().addListener((obs, oldValue, newValue) -> refreshSavedProblems());
        viewModel.currentDocumentProperty().addListener((obs, oldValue, newValue) -> {
            refreshNewProblem();
            refreshChapterOptions();
            refreshSavedProblems();
        });
        refreshChapterOptions();
        refreshNewProblem();
        refreshSavedProblems();
    }

    private void configureDetailLabels() {
        selectionHint.setWrapText(true);
        selectionHint.getStyleClass().add("document-technical-problem-hint");
        savedDetail.setWrapText(true);
        savedDetail.getStyleClass().add("document-technical-problem-detail");
    }

    private void configureFilters() {
        search.setPromptText("Buscar problemas");
        pageFrom.setPromptText("Pag. desde");
        pageTo.setPromptText("Pag. hasta");
        blockQuery.setPromptText("Bloque B0012");
        StudioFormControls.textInput(search, "Buscar por titulo, enunciado, fuente, solucion o notas.");
        StudioFormControls.textInput(pageFrom, "Pagina inicial de las fuentes del problema.");
        StudioFormControls.textInput(pageTo, "Pagina final de las fuentes del problema.");
        StudioFormControls.textInput(blockQuery, "Filtrar por id de bloque fuente, por ejemplo B0012.");
        StudioFormControls.combo(chapterFilter, "Filtrar por capitulo o rango derivado del indice documental.");
        StudioFormControls.combo(statusFilter, "Filtrar por estado de solucion o crops.");
        statusFilter.setValue(StudyProblemStatusFilter.ALL);
        statusFilter.setConverter(new StringConverter<>() {
            @Override
            public String toString(StudyProblemStatusFilter status) {
                return status == null ? "" : status.displayName();
            }

            @Override
            public StudyProblemStatusFilter fromString(String value) {
                return statusFilter.getValue();
            }
        });
        chapterFilter.setConverter(new StringConverter<>() {
            @Override
            public String toString(ChapterOption option) {
                return option == null ? "" : option.displayName();
            }

            @Override
            public ChapterOption fromString(String value) {
                return chapterFilter.getValue();
            }
        });
        search.textProperty().addListener((obs, oldValue, newValue) -> refreshSavedProblems());
        pageFrom.textProperty().addListener((obs, oldValue, newValue) -> refreshSavedProblems());
        pageTo.textProperty().addListener((obs, oldValue, newValue) -> refreshSavedProblems());
        blockQuery.textProperty().addListener((obs, oldValue, newValue) -> refreshSavedProblems());
        chapterFilter.valueProperty().addListener((obs, oldValue, newValue) -> refreshSavedProblems());
        statusFilter.valueProperty().addListener((obs, oldValue, newValue) -> refreshSavedProblems());
    }

    private VBox savedFilters() {
        javafx.scene.layout.HBox pageRow = new javafx.scene.layout.HBox(8, pageFrom, pageTo);
        javafx.scene.layout.HBox.setHgrow(pageFrom, Priority.ALWAYS);
        javafx.scene.layout.HBox.setHgrow(pageTo, Priority.ALWAYS);
        javafx.scene.layout.HBox queryRow = new javafx.scene.layout.HBox(8, blockQuery, statusFilter);
        javafx.scene.layout.HBox.setHgrow(blockQuery, Priority.ALWAYS);
        javafx.scene.layout.HBox.setHgrow(statusFilter, Priority.ALWAYS);
        VBox filters = new VBox(7, search, chapterFilter, pageRow, queryRow);
        filters.getStyleClass().add("document-technical-problem-filters");
        return filters;
    }

    private void configureSavedProblemsList() {
        savedProblems.setPrefHeight(170);
        savedProblems.getStyleClass().add("document-technical-problem-list");
        savedProblems.setCellFactory(view -> new ListCell<>() {
            @Override
            protected void updateItem(StudyProblemListItem item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : listText(item));
            }
        });
        savedProblems.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> refreshSavedDetail());
    }

    private void configureSourceNavigation() {
        StudioFormControls.combo(sourceSelector, "Fuente guardada del problema seleccionado.");
        sourceSelector.setConverter(new StringConverter<>() {
            @Override
            public String toString(StudyProblemSourceProjection source) {
                return source == null ? "" : sourceLabel(source);
            }

            @Override
            public StudyProblemSourceProjection fromString(String value) {
                return sourceSelector.getValue();
            }
        });
        sourceSelector.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> updateSourceNavigationState());
        goToSource.setOnAction(event -> goToSelectedSource());
    }

    private void configureSavedActions() {
        editSaved.setOnAction(event -> editSelectedProblem());
        exportPng.setOnAction(event -> exportSelectedPng());
        exportText.setOnAction(event -> exportSelectedText());
        deleteSaved.setOnAction(event -> deleteSelectedProblem());
        exportAllPng.setOnAction(event -> exportAllProblems());
        exportAllPdf.setOnAction(event -> exportAllProblemsPdf());
        createPdfFromImages.setOnAction(event -> createPdfSourceFromImages());
    }

    private VBox sourceNavigation() {
        VBox row = new VBox(8, sourceSelector, goToSource);
        row.getStyleClass().add("document-technical-problem-actions");
        sourceSelector.setMaxWidth(Double.MAX_VALUE);
        fullWidth(goToSource);
        return row;
    }

    private VBox savedActions() {
        VBox row = new VBox(8, exportAllPng, exportAllPdf, editSaved, exportPng, exportText, deleteSaved);
        row.getStyleClass().add("document-technical-problem-actions");
        for (Button button : List.of(exportAllPng, exportAllPdf, editSaved, exportPng, exportText, deleteSaved)) {
            fullWidth(button);
        }
        return row;
    }

    private VBox sourcePdfActions() {
        VBox row = new VBox(8, createPdfFromImages);
        row.getStyleClass().add("document-technical-problem-actions");
        fullWidth(createPdfFromImages);
        return row;
    }

    private static Label section(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("document-technical-problem-section");
        return label;
    }

    private void refreshNewProblem() {
        if (currentDocumentIsPdf()) {
            boolean active = viewModel.technicalProblemPreparationActiveProperty().get();
            List<PdfRegionCaptureDraft> regions = selectedPdfRegionsSupplier.get();
            status.setText(active
                    ? "Seleccion de capturas activa sobre el PDF."
                    : "Activa la seleccion y arrastra sobre la pagina para capturar el enunciado.");
            count.setText(regions.size() + " captura(s) PDF seleccionadas");
            if (regions.isEmpty()) {
                selectionHint.setText("Arrastra un rectangulo sobre la pagina para capturar el enunciado, una formula o una figura.");
                return;
            }
            String pages = regions.stream()
                    .map(region -> Integer.toString(region.sourcePage()))
                    .distinct()
                    .collect(java.util.stream.Collectors.joining(", "));
            selectionHint.setText("Listo para generar problema con " + regions.size()
                    + " captura(s) PDF" + (pages.isBlank() ? "." : " en pagina(s) " + pages + "."));
            return;
        }
        status.setText(viewModel.technicalProblemPreparationActiveProperty().get()
                ? "Seleccion activa en el lector."
                : "Abre el modo de seleccion para asociar fragmentos al problema.");
        List<DocumentBlock> blocks = selectedBlocksSupplier.get();
        count.setText(blocks.size() + " bloque(s) seleccionados");
        if (blocks.isEmpty()) {
            selectionHint.setText("Selecciona uno o varios fragmentos del documento y luego genera el problema.");
            return;
        }
        selectionHint.setText("Listo para generar problema con " + blocks.size() + " fragmento(s).");
    }

    private boolean currentDocumentIsPdf() {
        ReadableDocument document = viewModel.currentDocumentProperty().get();
        return document != null && document.format() == SourceDocumentFormat.PDF;
    }

    private void refreshSavedProblems() {
        String selected = selectedProblemId();
        projection = viewModel.projectWorkspace().documentStudy().buildStudyProblemsProjection()
                .build(viewModel.currentProject().orElse(null), viewModel.currentProjectDirectory().orElse(null), currentFilter());
        savedProblems.setItems(FXCollections.observableArrayList(projection.problems()));
        boolean hasProblems = !projection.problems().isEmpty();
        savedProblems.setVisible(hasProblems);
        savedProblems.setManaged(hasProblems);
        if (!selected.isBlank()) {
            savedProblems.getItems().stream()
                    .filter(item -> item.id().equals(selected))
                    .findFirst()
                    .ifPresent(item -> savedProblems.getSelectionModel().select(item));
        }
        if (savedProblems.getSelectionModel().getSelectedItem() == null && !savedProblems.getItems().isEmpty()) {
            savedProblems.getSelectionModel().selectFirst();
        }
        int shown = projection.problems().size();
        int total = projection.totalProblemCount();
        if (total == 0) {
            savedStatus.setText("Sin problemas guardados todavia.");
        } else {
            savedStatus.setText(shown + " de " + total + " problema(s)");
        }
        refreshSavedDetail();
    }

    private StudyProblemFilter currentFilter() {
        ChapterOption chapter = chapterFilter.getValue();
        Integer from = parsePositiveInt(pageFrom.getText()).orElse(chapter == null ? null : chapter.pageFrom());
        Integer to = parsePositiveInt(pageTo.getText()).orElse(chapter == null ? null : chapter.pageTo());
        return new StudyProblemFilter(search.getText(), from, to, blockQuery.getText(), statusFilter.getValue());
    }

    private void refreshSavedDetail() {
        StudyProblemDetail detail = selectedDetail();
        editSaved.setDisable(detail == null);
        deleteSaved.setDisable(detail == null);
        exportPng.setDisable(detail == null || !detail.hasSolutionImage());
        exportText.setDisable(detail == null || !detail.hasSolutionText());
        if (detail == null) {
            sourceSelector.setItems(FXCollections.observableArrayList());
            savedDetail.setText("Selecciona un problema guardado para ver estado y acciones.");
            updateSourceNavigationState();
            return;
        }
        sourceSelector.setItems(FXCollections.observableArrayList(detail.sources()));
        if (!detail.sources().isEmpty()) {
            sourceSelector.getSelectionModel().selectFirst();
        }
        savedDetail.setText(detail.title()
                + "\nActualizado: " + DATE_FORMAT.format(detail.updatedAt())
                + "\nFuentes: " + detail.sources().size()
                + "\nSolucion texto: " + (detail.hasSolutionText() ? "si" : "no")
                + "\nSolucion lienzo: " + (detail.hasSolutionImage() ? "si" : "no")
                + "\nNotas: " + (detail.notes().isBlank() ? "no" : "si"));
        updateSourceNavigationState();
    }

    private void refreshChapterOptions() {
        ChapterOption selected = chapterFilter.getValue();
        List<ChapterOption> options = chapterOptions(viewModel.currentDocumentProperty().get());
        chapterFilter.setItems(FXCollections.observableArrayList(options));
        ChapterOption next = options.stream()
                .filter(option -> option.sameRange(selected))
                .findFirst()
                .orElse(options.getFirst());
        chapterFilter.getSelectionModel().select(next);
        chapterFilter.setDisable(options.size() <= 1);
    }

    private List<ChapterOption> chapterOptions(ReadableDocument document) {
        List<ChapterOption> options = new ArrayList<>();
        options.add(ChapterOption.all());
        if (document == null) {
            return options;
        }
        DocumentOutlineProjection outline = viewModel.projectWorkspace().document().buildDocumentOutline().build(document);
        List<ChapterAnchor> anchors = new ArrayList<>();
        collectChapterAnchors(outline.entries(), anchors);
        anchors = anchors.stream()
                .sorted(Comparator.comparingInt(ChapterAnchor::page))
                .toList();
        for (int i = 0; i < anchors.size(); i++) {
            ChapterAnchor anchor = anchors.get(i);
            Integer to = i + 1 < anchors.size() ? Math.max(anchor.page(), anchors.get(i + 1).page() - 1) : null;
            options.add(new ChapterOption(anchor.label(), anchor.page(), to));
        }
        return options;
    }

    private static void collectChapterAnchors(List<DocumentOutlineEntry> entries, List<ChapterAnchor> anchors) {
        for (DocumentOutlineEntry entry : entries == null ? List.<DocumentOutlineEntry>of() : entries) {
            parsePositiveInt(entry.sourcePage()).ifPresent(page -> anchors.add(new ChapterAnchor(cleanOutlineLabel(entry.label()), page)));
            collectChapterAnchors(entry.children(), anchors);
        }
    }

    private static String cleanOutlineLabel(String label) {
        String normalized = label == null ? "" : label.replaceAll("\\s+", " ").strip();
        if (normalized.length() <= 70) {
            return normalized;
        }
        return normalized.substring(0, 70).strip() + "...";
    }

    private void editSelectedProblem() {
        StudyProblemDetail detail = selectedDetail();
        if (detail == null) {
            return;
        }
        TechnicalProblemDialog.showForEdit(getScene() == null ? null : getScene().getWindow(), detail,
                        viewModel.inkInputProviders().create(DrawingFeatureCatalog.DOCUMENT_PROBLEM),
                        viewModel.drawingFeatures().require(DrawingFeatureCatalog.DOCUMENT_PROBLEM))
                .ifPresent(result -> runAction(() -> {
                    viewModel.updateTechnicalProblemSolution(detail.id(), result.solutionText(), result.canvasSnapshot(), result.notes(), result.canvasStateJson());
                    refreshSavedProblems();
                }));
    }

    private void exportSelectedPng() {
        StudyProblemDetail detail = selectedDetail();
        if (detail == null) {
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Exportar solucion PNG");
        chooser.setInitialFileName(detail.id().toLowerCase(java.util.Locale.ROOT) + "-solucion.png");
        chooser.getExtensionFilters().setAll(new FileChooser.ExtensionFilter("PNG (*.png)", "*.png"));
        File file = chooser.showSaveDialog(getScene() == null ? null : getScene().getWindow());
        if (file != null) {
            runAction(() -> viewModel.exportTechnicalProblemImage(detail.id(), file.toPath()));
        }
    }

    private void exportSelectedText() {
        StudyProblemDetail detail = selectedDetail();
        if (detail == null) {
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Exportar solucion textual");
        chooser.setInitialFileName(detail.id().toLowerCase(java.util.Locale.ROOT) + "-solucion.txt");
        chooser.getExtensionFilters().setAll(new FileChooser.ExtensionFilter("Texto (*.txt)", "*.txt"));
        File file = chooser.showSaveDialog(getScene() == null ? null : getScene().getWindow());
        if (file != null) {
            runAction(() -> viewModel.exportTechnicalProblemText(detail.id(), file.toPath()));
        }
    }

    private void exportAllProblems() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Exportar todos los problemas tecnicos");
        File folder = chooser.showDialog(getScene() == null ? null : getScene().getWindow());
        if (folder != null) {
            runAction(() -> viewModel.exportAllTechnicalProblemImages(folder.toPath()));
        }
    }

    private void exportAllProblemsPdf() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Exportar ejercicios en PDF");
        chooser.setInitialFileName("ejercicios-tecnicos.pdf");
        chooser.getExtensionFilters().setAll(new FileChooser.ExtensionFilter("PDF (*.pdf)", "*.pdf"));
        File file = chooser.showSaveDialog(getScene() == null ? null : getScene().getWindow());
        if (file != null) {
            runAction(() -> viewModel.exportAllTechnicalProblemImagesPdf(file.toPath()));
        }
    }

    private void createPdfSourceFromImages() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Crear fuente PDF desde carpeta de imagenes");
        File folder = chooser.showDialog(getScene() == null ? null : getScene().getWindow());
        if (folder != null) {
            runAction(() -> viewModel.createPdfSourceFromImageFolder(folder.toPath()));
        }
    }

    private void deleteSelectedProblem() {
        StudyProblemDetail detail = selectedDetail();
        if (detail == null) {
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "", ButtonType.CANCEL, ButtonType.OK);
        confirm.setTitle("Eliminar problema tecnico");
        confirm.setHeaderText("Eliminar problema guardado");
        Label message = new Label("Eliminar " + detail.title()
                + " del proyecto? Tambien se quitaran sus assets de estudio y archivos generados dentro de la carpeta del proyecto.");
        message.setWrapText(true);
        message.setPrefWidth(380);
        confirm.getDialogPane().setContent(message);
        if (getScene() != null) {
            confirm.initOwner(getScene().getWindow());
        }
        if (confirm.showAndWait().filter(ButtonType.OK::equals).isPresent()) {
            runAction(() -> {
                viewModel.deleteTechnicalProblem(detail.id());
                refreshSavedProblems();
            });
        }
    }

    private void goToSelectedSource() {
        StudyProblemSourceProjection source = sourceSelector.getSelectionModel().getSelectedItem();
        if (source == null || source.blockId().isBlank()) {
            return;
        }
        viewModel.selectDocumentBlock(source.blockId());
        viewModel.updateStatusMessage("Fuente del problema seleccionada: " + sourceLabel(source) + ".");
    }

    private void updateSourceNavigationState() {
        goToSource.setDisable(sourceSelector.getSelectionModel().getSelectedItem() == null);
        sourceSelector.setDisable(sourceSelector.getItems().isEmpty());
    }

    private StudyProblemDetail selectedDetail() {
        StudyProblemListItem item = savedProblems.getSelectionModel().getSelectedItem();
        return item == null ? null : projection.detail(item.id()).orElse(null);
    }

    private String selectedProblemId() {
        StudyProblemListItem item = savedProblems.getSelectionModel().getSelectedItem();
        return item == null ? "" : item.id();
    }

    private void runAction(ProblemAction action) {
        try {
            action.run();
        } catch (java.io.IOException | RuntimeException ex) {
            viewModel.updateStatusMessage("No se pudo actualizar problema tecnico: " + ex.getMessage());
        }
    }

    private static String listText(StudyProblemListItem item) {
        String pages = item.sourcePageSummary();
        return item.title()
                + "\n" + item.badges() + (pages.isBlank() ? "" : " - " + pages)
                + (item.preview().isBlank() ? "" : "\n" + item.preview());
    }

    private static String sourceLabel(StudyProblemSourceProjection source) {
        String page = source.sourcePage().isBlank() ? "" : " - p. " + source.sourcePage();
        return source.blockId() + page;
    }

    private static void fullWidth(Button button) {
        button.setMaxWidth(Double.MAX_VALUE);
        button.setMinHeight(38);
        button.setWrapText(true);
    }

    private static Optional<Integer> parsePositiveInt(String value) {
        if (value == null || !value.strip().matches("\\d+")) {
            return Optional.empty();
        }
        int parsed = Integer.parseInt(value.strip());
        return parsed > 0 ? Optional.of(parsed) : Optional.empty();
    }

    @FunctionalInterface
    private interface ProblemAction {
        void run() throws java.io.IOException;
    }

    private record ChapterAnchor(String label, int page) {
    }

    private record ChapterOption(String label, Integer pageFrom, Integer pageTo) {
        private static ChapterOption all() {
            return new ChapterOption("Todos los capitulos", null, null);
        }

        private String displayName() {
            if (pageFrom == null) {
                return label;
            }
            return pageTo == null ? label + " - desde p. " + pageFrom : label + " - p. " + pageFrom + "-" + pageTo;
        }

        private boolean sameRange(ChapterOption other) {
            return other != null && Objects.equals(pageFrom, other.pageFrom) && Objects.equals(pageTo, other.pageTo);
        }
    }
}

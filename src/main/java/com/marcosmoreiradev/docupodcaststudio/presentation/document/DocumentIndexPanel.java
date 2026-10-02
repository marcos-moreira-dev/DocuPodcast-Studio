package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioFeedbackControls;

import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioCollectionControls;

import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioFormControls;

import com.marcosmoreiradev.docupodcaststudio.application.document.BuildDocumentOutlineUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.document.BuildPdfEnhancedOutlineUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.document.DocumentOutlineEntry;
import com.marcosmoreiradev.docupodcaststudio.application.document.DocumentOutlineProjection;
import com.marcosmoreiradev.docupodcaststudio.application.document.DocumentOutlineOrigin;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfTextSearchProjection;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfTextSearchRequest;
import com.marcosmoreiradev.docupodcaststudio.application.document.PreparedPdfSource;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfTextSearchResult;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfVisualTextHighlight;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfPreparationScope;
import com.marcosmoreiradev.docupodcaststudio.application.document.SearchPdfTextUseCase;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlockType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.ActionButtonFactory;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/** Navigable document outline rendered from an application projection without altering the source document. */
public final class DocumentIndexPanel extends BorderPane {
    private com.marcosmoreiradev.docupodcaststudio.application.document.PdfReadingPreferencesUseCase readingPreferences;
    private final ComboBox<com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfReadingStrategy> readingStrategy =
            StudioFormControls.comboBox();
    private final Label readingDescription = note("");
    private final VBox readingPreferencesBox = new VBox(6);
    private boolean syncingPreferences;

    public DocumentIndexPanel withReadingPreferences(
            com.marcosmoreiradev.docupodcaststudio.application.document.PdfReadingPreferencesUseCase preferences) {
        readingPreferences = Objects.requireNonNull(preferences);
        readingStrategy.getItems().setAll(
                com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfReadingStrategy.values());
        readingStrategy.setMaxWidth(Double.MAX_VALUE);
        readingStrategy.setId("pdf-reading-strategy");
        Label label = new Label("Cómo leer este PDF");
        label.setLabelFor(readingStrategy);
        readingPreferencesBox.getChildren().setAll(label, readingStrategy, readingDescription);
        readingStrategy.setOnAction(event -> {
            if (syncingPreferences || readingStrategy.getValue() == null) return;
            PreparedPdfSource source = pdfSourceProperty == null ? null : pdfSourceProperty.getValue();
            if (source == null) return;
            try {
                readingPreferences.update(source.workspace(), readingStrategy.getValue());
                readingDescription.setText(readingStrategy.getValue().description()
                        + " Se aplicará al preparar el documento.");
            } catch (java.io.IOException failure) {
                refreshReadingPreferences(source);
                readingDescription.setText("No se pudo guardar la preferencia: " + failure.getMessage());
            }
        });
        renderActive();
        return this;
    }

    private void refreshReadingPreferences(PreparedPdfSource source) {
        if (readingPreferences == null) return;
        syncingPreferences = true;
        try {
            var preference = readingPreferences.read(source.workspace()).readingStrategy();
            readingStrategy.setValue(preference);
            readingStrategy.setDisable(false);
            readingDescription.setText(preference.description());
        } catch (java.io.IOException failure) {
            readingStrategy.setDisable(true);
            readingDescription.setText("No se pudo leer la preferencia del documento.");
        } finally {
            syncingPreferences = false;
        }
    }
    private final ObservableValue<ReadableDocument> documentProperty;
    private final ObservableValue<PreparedPdfSource> pdfSourceProperty;
    private final ObservableValue<String> selectedBlockIdProperty;
    private final Consumer<String> onBlockSelected;
    private final Consumer<PdfVisualTextHighlight> onPdfHighlightSelected;
    private final IntConsumer onPdfPageSelected;
    private final BuildDocumentOutlineUseCase buildOutline;
    private final BuildPdfEnhancedOutlineUseCase buildEnhancedOutline;
    private final SearchPdfTextUseCase searchPdfText;
    private final PdfIndexPreparationHandler prepareForSearch;
    private final VBox header = new VBox(4);
    private final TextField pdfSearch = StudioFormControls.textField();
    private final TextField pdfPageJump = StudioFormControls.textField();
    private final Button nativeSearchButton = ActionButtonFactory.primary("Buscar");
    private final Button clearSearchButton = ActionButtonFactory.secondary("Limpiar");
    private final Button expandSearchButton = ActionButtonFactory.secondary("Ampliar búsqueda");
    private final ComboBox<PdfPreparationScope> searchScope = StudioFormControls.comboBox();
    private final TextField rangeStart = StudioFormControls.textField();
    private final TextField rangeEnd = StudioFormControls.textField();
    private final Button pdfPageJumpButton = ActionButtonFactory.primary("Ir a pagina");
    private final ProgressIndicator searchProgress = StudioFeedbackControls.progressIndicator();
    private final Label searchStatus = note("");
    private final Label pdfPageJumpStatus = note("");
    private final HBox searchActions = new HBox(6);
    private final HBox pageJumpActions = new HBox(6);
    private final VBox searchBox = new VBox(6);
    private final VBox pageJumpBox = new VBox(6);
    private final TextField bookmarkName = StudioFormControls.textField();
    private final TextField bookmarkPage = StudioFormControls.textField();
    private final Button addBookmarkButton = ActionButtonFactory.primary("Agregar marcador");
    private final Button goBookmarkButton = ActionButtonFactory.secondary("Ir");
    private final Button deleteBookmarkButton = ActionButtonFactory.danger("Eliminar");
    private final ComboBox<DocumentBookmarkEntry> bookmarkChoice = StudioFormControls.comboBox();
    private final VBox bookmarksBox = new VBox(6);
    private final TreeView<IndexEntry> tree = StudioCollectionControls.treeView();
    private boolean syncingSelection;
    private boolean selectionUpdateQueued;
    private String pendingSelectionBlockId = "";
    private String lastSelectedIndexBlockId = "";
    private String lastScrolledIndexBlockId = "";
    private String lastStyledSelectionBlockId = "";
    private DocumentOutlineProjection currentProjection;
    private final List<IndexedBlock> indexedBlocks = new ArrayList<>();
    private final Map<String, Integer> blockSourceIndexById = new HashMap<>();
    private final Map<String, TreeItem<IndexEntry>> indexedItemsByBlockId = new HashMap<>();
    private final Map<String, List<DocumentBookmarkEntry>> bookmarksByDocumentKey = new HashMap<>();

    public DocumentIndexPanel(ObservableValue<ReadableDocument> documentProperty,
                              ObservableValue<String> selectedBlockIdProperty,
                              Consumer<String> onBlockSelected,
                              BuildDocumentOutlineUseCase buildOutline) {
        this(documentProperty, selectedBlockIdProperty, onBlockSelected, buildOutline,
                null, null, null, null, null, null);
    }

    public DocumentIndexPanel(ObservableValue<ReadableDocument> documentProperty,
                              ObservableValue<String> selectedBlockIdProperty,
                              Consumer<String> onBlockSelected,
                              BuildDocumentOutlineUseCase buildOutline,
                              BuildPdfEnhancedOutlineUseCase buildEnhancedOutline,
                              SearchPdfTextUseCase searchPdfText,
                              Consumer<PdfVisualTextHighlight> onPdfHighlightSelected,
                              IntConsumer onPdfPageSelected) {
        this(documentProperty, selectedBlockIdProperty, onBlockSelected, buildOutline,
                buildEnhancedOutline, searchPdfText, onPdfHighlightSelected, onPdfPageSelected,
                null, null);
    }

    public DocumentIndexPanel(ObservableValue<ReadableDocument> documentProperty,
                              ObservableValue<String> selectedBlockIdProperty,
                              Consumer<String> onBlockSelected,
                              BuildDocumentOutlineUseCase buildOutline,
                              BuildPdfEnhancedOutlineUseCase buildEnhancedOutline,
                              SearchPdfTextUseCase searchPdfText,
                              Consumer<PdfVisualTextHighlight> onPdfHighlightSelected,
                              IntConsumer onPdfPageSelected,
                              PdfIndexPreparationHandler prepareForSearch) {
        this(documentProperty, selectedBlockIdProperty, onBlockSelected, buildOutline,
                buildEnhancedOutline, searchPdfText, onPdfHighlightSelected, onPdfPageSelected,
                prepareForSearch, null);
    }

    public DocumentIndexPanel(ObservableValue<ReadableDocument> documentProperty,
                              ObservableValue<PreparedPdfSource> pdfSourceProperty,
                              ObservableValue<String> selectedBlockIdProperty,
                              Consumer<String> onBlockSelected,
                              BuildDocumentOutlineUseCase buildOutline,
                              BuildPdfEnhancedOutlineUseCase buildEnhancedOutline,
                              SearchPdfTextUseCase searchPdfText,
                              Consumer<PdfVisualTextHighlight> onPdfHighlightSelected,
                              IntConsumer onPdfPageSelected,
                              PdfIndexPreparationHandler prepareForSearch) {
        this(documentProperty, selectedBlockIdProperty, onBlockSelected, buildOutline,
                buildEnhancedOutline, searchPdfText, onPdfHighlightSelected, onPdfPageSelected,
                prepareForSearch, pdfSourceProperty);
    }

    private DocumentIndexPanel(ObservableValue<ReadableDocument> documentProperty,
                               ObservableValue<String> selectedBlockIdProperty,
                               Consumer<String> onBlockSelected,
                               BuildDocumentOutlineUseCase buildOutline,
                               BuildPdfEnhancedOutlineUseCase buildEnhancedOutline,
                               SearchPdfTextUseCase searchPdfText,
                               Consumer<PdfVisualTextHighlight> onPdfHighlightSelected,
                               IntConsumer onPdfPageSelected,
                               PdfIndexPreparationHandler prepareForSearch,
                               ObservableValue<PreparedPdfSource> pdfSourceProperty) {
        this.documentProperty = Objects.requireNonNull(documentProperty, "documentProperty");
        this.pdfSourceProperty = pdfSourceProperty;
        this.selectedBlockIdProperty = selectedBlockIdProperty;
        this.onBlockSelected = Objects.requireNonNull(onBlockSelected, "onBlockSelected");
        this.buildOutline = Objects.requireNonNull(buildOutline, "buildOutline");
        this.buildEnhancedOutline = buildEnhancedOutline;
        this.searchPdfText = searchPdfText;
        this.prepareForSearch = prepareForSearch;
        this.onPdfHighlightSelected = onPdfHighlightSelected;
        this.onPdfPageSelected = onPdfPageSelected;
        getStyleClass().add("document-index-panel");
        header.getStyleClass().add("document-index-header");
        pdfSearch.getStyleClass().add("document-index-search-field");
        nativeSearchButton.getStyleClass().add("document-index-search-button");
        clearSearchButton.getStyleClass().add("document-index-search-button");
        expandSearchButton.getStyleClass().add("document-index-search-button");
        pdfPageJump.getStyleClass().add("document-index-search-field");
        pdfPageJumpButton.getStyleClass().add("document-index-search-button");
        bookmarkName.getStyleClass().add("document-index-search-field");
        bookmarkPage.getStyleClass().add("document-index-search-field");
        bookmarkChoice.getStyleClass().add("document-index-search-field");
        addBookmarkButton.getStyleClass().add("document-index-search-button");
        goBookmarkButton.getStyleClass().add("document-index-search-button");
        deleteBookmarkButton.getStyleClass().add("document-index-search-button");
        Tooltip.install(nativeSearchButton, new Tooltip("Buscar en el texto ya disponible del PDF."));
        Tooltip.install(clearSearchButton, new Tooltip("Volver al temario o a las paginas del PDF."));
        Tooltip.install(expandSearchButton, new Tooltip("Preparar explícitamente más páginas y repetir la búsqueda."));
        Tooltip.install(pdfPageJumpButton, new Tooltip("Saltar directamente a una pagina del PDF."));
        Tooltip.install(addBookmarkButton, new Tooltip("Guardar una pagina con un nombre corto para volver despues."));
        Tooltip.install(goBookmarkButton, new Tooltip("Saltar al marcador seleccionado."));
        Tooltip.install(deleteBookmarkButton, new Tooltip("Quitar el marcador seleccionado de esta sesion."));
        searchProgress.setMaxSize(18, 18);
        searchProgress.setVisible(false);
        searchProgress.setManaged(false);
        searchActions.getStyleClass().add("document-index-search-actions");
        searchScope.getItems().setAll(PdfPreparationScope.CURRENT_SECTION,
                PdfPreparationScope.FROM_CURRENT_TO_SECTION_END,
                PdfPreparationScope.PAGE_RANGE,
                PdfPreparationScope.WHOLE_DOCUMENT);
        searchScope.setValue(PdfPreparationScope.CURRENT_SECTION);
        rangeStart.setPromptText("Desde");
        rangeEnd.setPromptText("Hasta");
        rangeStart.setPrefColumnCount(5);
        rangeEnd.setPrefColumnCount(5);
        HBox range = new HBox(6, rangeStart, rangeEnd);
        range.visibleProperty().bind(searchScope.valueProperty().isEqualTo(PdfPreparationScope.PAGE_RANGE));
        range.managedProperty().bind(range.visibleProperty());
        searchActions.getChildren().setAll(nativeSearchButton, clearSearchButton, searchProgress);
        searchBox.getStyleClass().add("document-index-search");
        if (prepareForSearch == null) {
            searchBox.getChildren().setAll(pdfSearch, searchActions, searchStatus);
        } else {
            searchBox.getChildren().setAll(pdfSearch, searchActions, searchScope, range,
                    expandSearchButton, searchStatus);
        }
        pageJumpActions.getStyleClass().add("document-index-search-actions");
        pageJumpActions.getChildren().setAll(pdfPageJumpButton);
        pageJumpBox.getStyleClass().add("document-index-page-jump");
        pageJumpBox.getChildren().setAll(pdfPageJump, pageJumpActions, pdfPageJumpStatus);
        bookmarkName.setPromptText("Nombre del marcador");
        bookmarkPage.setPromptText("Pagina");
        bookmarkChoice.setPromptText("Marcadores del documento");
        HBox bookmarkButtons = new HBox(6, goBookmarkButton, deleteBookmarkButton);
        bookmarkButtons.getStyleClass().add("document-index-search-actions");
        Label bookmarkTitle = new Label("Marcadores");
        bookmarkTitle.getStyleClass().add("document-side-title");
        bookmarksBox.getStyleClass().add("document-index-bookmarks");
        bookmarksBox.getChildren().setAll(bookmarkTitle, bookmarkName, bookmarkPage, addBookmarkButton, bookmarkChoice, bookmarkButtons);
        tree.getStyleClass().add("document-index-tree");
        tree.setShowRoot(false);
        tree.setCellFactory(view -> new TreeCell<>() {
            @Override
            protected void updateItem(IndexEntry item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeIf(style -> style.startsWith("document-index-cell-") || style.equals("document-index-cell-selected"));
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                setText(item.label());
                getStyleClass().add("document-index-cell-" + item.kind().name().toLowerCase(Locale.ROOT));
                if (item.blockId().equals(currentSelectedBlockId())) {
                    getStyleClass().add("document-index-cell-selected");
                }
            }
        });
        tree.getSelectionModel().selectedItemProperty().addListener((obs, oldItem, newItem) -> {
            if (syncingSelection || newItem == null || newItem.getValue() == null) {
                return;
            }
            IndexEntry value = newItem.getValue();
            if (value.highlight() != null && onPdfHighlightSelected != null) {
                onPdfHighlightSelected.accept(value.highlight());
            }
            if (!value.blockId().isBlank()) {
                onBlockSelected.accept(value.blockId());
            }
        });
        nativeSearchButton.setOnAction(event -> runPdfSearch());
        clearSearchButton.setOnAction(event -> renderActive());
        expandSearchButton.setOnAction(event -> expandPdfSearch());
        pdfPageJumpButton.setOnAction(event -> goToPdfPage());
        pdfSearch.setOnAction(event -> runPdfSearch());
        pdfPageJump.setOnAction(event -> goToPdfPage());
        addBookmarkButton.setOnAction(event -> addBookmark());
        goBookmarkButton.setOnAction(event -> goToBookmark());
        deleteBookmarkButton.setOnAction(event -> deleteBookmark());
        setTop(header);
        setCenter(tree);
        documentProperty.addListener((obs, oldValue, newValue) -> renderActive());
        if (pdfSourceProperty != null) {
            pdfSourceProperty.addListener((obs, oldValue, newValue) -> renderActive());
        }
        if (selectedBlockIdProperty != null) {
            selectedBlockIdProperty.addListener((obs, oldValue, newValue) -> queueSelectionRefresh(newValue));
        }
        renderActive();
    }

    private void renderActive() {
        PreparedPdfSource source = pdfSourceProperty == null ? null : pdfSourceProperty.getValue();
        if (source != null) renderPdf(source);
        else render(documentProperty.getValue());
    }

    private void renderPdf(PreparedPdfSource source) {
        resetIndexCache(null);
        header.getChildren().clear();
        DocumentOutlineProjection projection = buildEnhancedOutline == null
                ? new DocumentOutlineProjection(DocumentOutlineOrigin.FLAT, "Índice del PDF",
                "No hay proyección de estructura disponible.", List.of(), 0, 0)
                : buildEnhancedOutline.build(source.workspace());
        currentProjection = projection;
        Label title = new Label("Índice del documento");
        title.getStyleClass().add("document-side-title");
        header.getChildren().addAll(title,
                note("Títulos y secciones del documento."), pdfSearchControls());
        if (readingPreferences != null) {
            refreshReadingPreferences(source);
            header.getChildren().add(readingPreferencesBox);
        }
        TreeItem<IndexEntry> root = outlineRoot(projection);
        tree.setRoot(root);
        expandFirstLevel(root);
        queueSelectionRefresh(currentSelectedBlockId());
    }

    private void render(ReadableDocument document) {
        resetIndexCache(document);
        header.getChildren().clear();
        DocumentOutlineProjection projection = buildOutline.build(document);
        currentProjection = projection;
        Label title = new Label(projection.title());
        title.getStyleClass().add("document-side-title");
        Label purpose = note(purposeText(document, projection));
        Label detail = note(projection.detail());
        header.getChildren().addAll(title, purpose, detail);
        if (document != null) {
            header.getChildren().add(bookmarkControls(document));
        }
        TreeItem<IndexEntry> root = outlineRoot(projection);
        tree.setRoot(root);
        expandFirstLevel(root);
        queueSelectionRefresh(currentSelectedBlockId());
    }

    private static Label note(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.getStyleClass().add("document-side-muted");
        return label;
    }

    private VBox pdfSearchControls() {
        pdfSearch.setPromptText("Buscar en PDF");
        return searchBox;
    }

    private VBox pdfPageJumpControls(DocumentOutlineProjection projection) {
        int pageCount = pageCount(projection);
        pdfPageJump.setPromptText(pageCount > 0 ? "Ir a pagina (1-" + pageCount + ")" : "Ir a pagina");
        if (pdfPageJumpStatus.getText() == null || pdfPageJumpStatus.getText().isBlank()) {
            pdfPageJumpStatus.setText("Escribe una pagina para saltar directamente.");
        }
        return pageJumpBox;
    }

    private VBox bookmarkControls(ReadableDocument document) {
        refreshBookmarkChoices(document);
        return bookmarksBox;
    }

    private static boolean strongProjection(DocumentOutlineProjection projection) {
        return projection != null
                && (projection.origin() == com.marcosmoreiradev.docupodcaststudio.application.document.DocumentOutlineOrigin.PDF_BOOKMARKS
                || projection.origin() == com.marcosmoreiradev.docupodcaststudio.application.document.DocumentOutlineOrigin.CONTENTS
                || projection.origin() == com.marcosmoreiradev.docupodcaststudio.application.document.DocumentOutlineOrigin.HEADINGS);
    }

    private static boolean betterProjection(DocumentOutlineProjection enhanced, DocumentOutlineProjection base) {
        if (enhanced == null || base == null) {
            return false;
        }
        if (enhanced.origin() == base.origin()) {
            return enhanced.indexedEntryCount() > base.indexedEntryCount();
        }
        return enhanced.origin() == com.marcosmoreiradev.docupodcaststudio.application.document.DocumentOutlineOrigin.CONTENTS;
    }

    private void applyProjection(DocumentOutlineProjection projection) {
        header.getChildren().clear();
        currentProjection = projection;
        Label title = new Label(projection.title());
        title.getStyleClass().add("document-side-title");
        header.getChildren().addAll(title,
                note(purposeText(documentProperty.getValue(), projection)),
                note(projection.detail()));
        if (documentProperty.getValue() != null) {
            header.getChildren().add(bookmarkControls(documentProperty.getValue()));
        }
        resetIndexCache(documentProperty.getValue());
        TreeItem<IndexEntry> root = outlineRoot(projection);
        tree.setRoot(root);
        expandFirstLevel(root);
        queueSelectionRefresh(currentSelectedBlockId());
    }

    private TreeItem<IndexEntry> outlineRoot(DocumentOutlineProjection projection) {
        TreeItem<IndexEntry> root = new TreeItem<>(IndexEntry.root());
        root.setExpanded(true);
        if (projection != null
                && projection.origin() == com.marcosmoreiradev.docupodcaststudio.application.document.DocumentOutlineOrigin.PDF_PAGES) {
            return root;
        }
        for (DocumentOutlineEntry entry : projection.entries()) {
            root.getChildren().add(itemFrom(entry));
        }
        if (root.getChildren().isEmpty()) {
            root.getChildren().add(new TreeItem<>(IndexEntry.message("Sin bloques navegables", "El documento no expuso entradas para indexar.")));
        }
        return root;
    }

    private void goToPdfPage() {
        DocumentOutlineProjection projection = currentProjection;
        if (projection == null) {
            pdfPageJumpStatus.setText("No hay PDF activo para navegar.");
            return;
        }
        String raw = pdfPageJump.getText() == null ? "" : pdfPageJump.getText().strip();
        int requested;
        try {
            requested = Integer.parseInt(raw);
        } catch (NumberFormatException ex) {
            pdfPageJumpStatus.setText("Escribe solo el numero de pagina.");
            return;
        }
        int pageCount = pageCount(projection);
        if (requested < 1 || (pageCount > 0 && requested > pageCount)) {
            pdfPageJumpStatus.setText(pageCount > 0 ? "Pagina valida: 1 a " + pageCount + "." : "Pagina fuera de rango.");
            return;
        }
        if (projection.origin() == com.marcosmoreiradev.docupodcaststudio.application.document.DocumentOutlineOrigin.PDF_PAGES
                && onPdfPageSelected != null) {
            onPdfPageSelected.accept(requested);
            pdfPageJumpStatus.setText("Pagina " + requested + ".");
            bookmarkPage.setText(Integer.toString(requested));
            return;
        }
        DocumentOutlineEntry target = nearestPageEntry(projection.entries(), requested);
        if (target == null || target.blockId().isBlank()) {
            pdfPageJumpStatus.setText("No hay ancla navegable para esa pagina.");
            return;
        }
        onBlockSelected.accept(target.blockId());
        pdfPageJumpStatus.setText("Pagina " + requested + ".");
    }

    private static int pageCount(DocumentOutlineProjection projection) {
        if (projection == null) {
            return 0;
        }
        int max = 0;
        if (projection.origin() == com.marcosmoreiradev.docupodcaststudio.application.document.DocumentOutlineOrigin.PDF_PAGES) {
            max = Math.max(max, projection.indexedEntryCount());
        }
        for (DocumentOutlineEntry entry : projection.entries()) {
            max = Math.max(max, maxSourcePage(entry));
        }
        return max;
    }

    private static int maxSourcePage(DocumentOutlineEntry entry) {
        if (entry == null) {
            return 0;
        }
        int max = parsePositiveInt(entry.sourcePage());
        for (DocumentOutlineEntry child : entry.children()) {
            max = Math.max(max, maxSourcePage(child));
        }
        return max;
    }

    private void addBookmark() {
        ReadableDocument document = documentProperty.getValue();
        if (document == null) {
            return;
        }
        int page = pageFromBookmarkField();
        if (page <= 0) {
            pdfPageJumpStatus.setText("Indica una pagina valida para el marcador.");
            return;
        }
        int pageCount = pageCount(currentProjection);
        if (pageCount > 0 && page > pageCount) {
            pdfPageJumpStatus.setText("Pagina valida: 1 a " + pageCount + ".");
            return;
        }
        String name = bookmarkName.getText() == null ? "" : bookmarkName.getText().strip();
        if (name.isBlank()) {
            name = "Pagina " + page;
        }
        String key = documentKey(document);
        List<DocumentBookmarkEntry> entries = new ArrayList<>(bookmarksByDocumentKey.getOrDefault(key, List.of()));
        DocumentBookmarkEntry next = new DocumentBookmarkEntry(name, page);
        entries.remove(next);
        entries.add(next);
        bookmarksByDocumentKey.put(key, entries);
        bookmarkName.clear();
        bookmarkPage.setText(Integer.toString(page));
        refreshBookmarkChoices(document);
        bookmarkChoice.getSelectionModel().select(next);
    }

    private void goToBookmark() {
        DocumentBookmarkEntry selected = bookmarkChoice.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        pdfPageJump.setText(Integer.toString(selected.page()));
        bookmarkPage.setText(Integer.toString(selected.page()));
        goToPdfPage();
    }

    private void deleteBookmark() {
        ReadableDocument document = documentProperty.getValue();
        DocumentBookmarkEntry selected = bookmarkChoice.getSelectionModel().getSelectedItem();
        if (document == null || selected == null) {
            return;
        }
        String key = documentKey(document);
        List<DocumentBookmarkEntry> entries = new ArrayList<>(bookmarksByDocumentKey.getOrDefault(key, List.of()));
        entries.remove(selected);
        bookmarksByDocumentKey.put(key, entries);
        refreshBookmarkChoices(document);
    }

    private int pageFromBookmarkField() {
        String raw = bookmarkPage.getText() == null || bookmarkPage.getText().isBlank()
                ? pdfPageJump.getText()
                : bookmarkPage.getText();
        if (raw == null || raw.isBlank()) {
            return 0;
        }
        try {
            return Math.max(0, Integer.parseInt(raw.strip()));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private void refreshBookmarkChoices(ReadableDocument document) {
        List<DocumentBookmarkEntry> entries = bookmarksByDocumentKey.getOrDefault(documentKey(document), List.of());
        bookmarkChoice.getItems().setAll(entries);
        boolean empty = entries.isEmpty();
        goBookmarkButton.setDisable(empty);
        deleteBookmarkButton.setDisable(empty);
    }

    private static String documentKey(ReadableDocument document) {
        if (document == null || document.sourcePath() == null) {
            return "";
        }
        return document.sourcePath().toAbsolutePath().normalize().toString();
    }

    private static DocumentOutlineEntry nearestPageEntry(List<DocumentOutlineEntry> entries, int page) {
        DocumentOutlineEntry exact = null;
        DocumentOutlineEntry closestBefore = null;
        DocumentOutlineEntry closestAfter = null;
        int closestBeforePage = 0;
        int closestAfterPage = Integer.MAX_VALUE;
        for (DocumentOutlineEntry entry : entries == null ? List.<DocumentOutlineEntry>of() : entries) {
            DocumentOutlineEntry candidate = nearestPageEntry(entry, page);
            int candidatePage = candidate == null ? 0 : parsePositiveInt(candidate.sourcePage());
            if (candidatePage == page) {
                exact = candidate;
                break;
            }
            if (candidatePage > 0 && candidatePage < page && candidatePage > closestBeforePage) {
                closestBefore = candidate;
                closestBeforePage = candidatePage;
            } else if (candidatePage > page && candidatePage < closestAfterPage) {
                closestAfter = candidate;
                closestAfterPage = candidatePage;
            }
        }
        return exact != null ? exact : (closestBefore != null ? closestBefore : closestAfter);
    }

    private static DocumentOutlineEntry nearestPageEntry(DocumentOutlineEntry entry, int page) {
        if (entry == null) {
            return null;
        }
        DocumentOutlineEntry best = entry;
        int bestDistance = distanceToPage(entry, page);
        for (DocumentOutlineEntry child : entry.children()) {
            DocumentOutlineEntry candidate = nearestPageEntry(child, page);
            int distance = distanceToPage(candidate, page);
            if (distance < bestDistance) {
                best = candidate;
                bestDistance = distance;
            }
        }
        return best;
    }

    private static int distanceToPage(DocumentOutlineEntry entry, int page) {
        int sourcePage = entry == null ? 0 : parsePositiveInt(entry.sourcePage());
        return sourcePage <= 0 ? Integer.MAX_VALUE : Math.abs(sourcePage - page);
    }

    private static int parsePositiveInt(String raw) {
        if (raw == null || raw.isBlank()) {
            return 0;
        }
        try {
            return Math.max(0, Integer.parseInt(raw.strip()));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private void runPdfSearch() {
        PreparedPdfSource source = pdfSourceProperty == null ? null : pdfSourceProperty.getValue();
        if (source == null || searchPdfText == null) {
            return;
        }
        String query = pdfSearch.getText() == null ? "" : pdfSearch.getText().strip();
        if (query.isBlank()) {
            searchStatus.setText("Escribe texto para buscar en el PDF.");
            return;
        }
        setSearchBusy(true, "Buscando en las páginas preparadas...");
        Task<PdfTextSearchProjection> task = new Task<>() {
            @Override
            protected PdfTextSearchProjection call() {
                return searchPdfText.search(new PdfTextSearchRequest(
                        source.workspace(), query, false, 80, 24));
            }
        };
        task.setOnSucceeded(event -> {
            if (pdfSourceProperty == null || source != pdfSourceProperty.getValue()) {
                return;
            }
            setSearchBusy(false, "");
            showSearchResults(task.getValue());
        });
        task.setOnFailed(event -> setSearchBusy(false,
                "No se pudo buscar en PDF: " + diagnostic(task.getException())));
        Thread worker = new Thread(task, "pdf-search-prepared-pages");
        worker.setDaemon(true);
        worker.start();
    }

    private void expandPdfSearch() {
        PreparedPdfSource source = pdfSourceProperty == null ? null : pdfSourceProperty.getValue();
        if (source == null || prepareForSearch == null) return;
        PdfPreparationScope scope = searchScope.getValue() == null
                ? PdfPreparationScope.CURRENT_SECTION : searchScope.getValue();
        int start = scope == PdfPreparationScope.PAGE_RANGE ? parsePositiveInt(rangeStart.getText()) : 0;
        int end = scope == PdfPreparationScope.PAGE_RANGE ? parsePositiveInt(rangeEnd.getText()) : 0;
        if (scope == PdfPreparationScope.PAGE_RANGE && (start <= 0 || end < start)) {
            searchStatus.setText("Indica un rango de páginas válido.");
            return;
        }
        expandSearchButton.setDisable(true);
        prepareForSearch.prepare(source, scope, start, end, () -> {
            expandSearchButton.setDisable(false);
            runPdfSearch();
        }, message -> {
            searchStatus.setText(message == null ? "" : message);
            if (message != null && (message.startsWith("No se pudo")
                    || message.startsWith("Preparación cancelada")
                    || message.startsWith("Indica"))) {
                expandSearchButton.setDisable(false);
            }
        });
    }

    private void setSearchBusy(boolean busy, String status) {
        searchProgress.setVisible(busy);
        searchProgress.setManaged(busy);
        nativeSearchButton.setDisable(busy);
        clearSearchButton.setDisable(busy);
        expandSearchButton.setDisable(busy);
        searchStatus.setText(status == null ? "" : status);
    }

    private void showSearchResults(PdfTextSearchProjection projection) {
        if (projection == null) {
            searchStatus.setText("Sin resultados.");
            return;
        }
        TreeItem<IndexEntry> root = new TreeItem<>(IndexEntry.root());
        root.setExpanded(true);
        for (PdfTextSearchResult result : projection.results()) {
            root.getChildren().add(new TreeItem<>(IndexEntry.searchResult(result)));
        }
        if (root.getChildren().isEmpty()) {
            root.getChildren().add(new TreeItem<>(IndexEntry.message(
                    "Sin resultados",
                    "No se encontró texto en las páginas ya preparadas.")));
        }
        tree.setRoot(root);
        expandFirstLevel(root);
        searchStatus.setText(projection.results().size() + " resultado(s) en "
                + projection.scannedPages() + " página(s) preparada(s).");
    }

    private static String purposeText(ReadableDocument document, DocumentOutlineProjection projection) {
        return "Origen del indice: " + (projection == null ? "" : projection.origin().label()) + ".";
    }

    private static String diagnostic(Throwable ex) {
        String message = ex == null ? "" : ex.getMessage();
        return message == null || message.isBlank() ? "error sin detalle." : message;
    }

    private TreeItem<IndexEntry> itemFrom(DocumentOutlineEntry entry) {
        TreeItem<IndexEntry> item = new TreeItem<>(IndexEntry.from(entry));
        if (entry.hasBlock()) {
            indexedBlocks.add(new IndexedBlock(entry.blockId(), entry.sourceIndex()));
            indexedItemsByBlockId.put(entry.blockId(), item);
        }
        for (DocumentOutlineEntry child : entry.children()) {
            item.getChildren().add(itemFrom(child));
        }
        return item;
    }

    private static void expandFirstLevel(TreeItem<IndexEntry> root) {
        if (root == null) {
            return;
        }
        root.setExpanded(true);
        for (TreeItem<IndexEntry> child : root.getChildren()) {
            child.setExpanded(true);
        }
    }

    private String currentSelectedBlockId() {
        if (selectedBlockIdProperty == null) {
            return "";
        }
        String value = selectedBlockIdProperty.getValue();
        return value == null ? "" : value;
    }

    private void resetIndexCache(ReadableDocument document) {
        indexedBlocks.clear();
        blockSourceIndexById.clear();
        indexedItemsByBlockId.clear();
        lastSelectedIndexBlockId = "";
        lastScrolledIndexBlockId = "";
        lastStyledSelectionBlockId = "";
        if (document == null) {
            return;
        }
        List<DocumentBlock> blocks = document.blocks();
        for (int i = 0; i < blocks.size(); i++) {
            String id = blocks.get(i).id();
            if (id != null && !id.isBlank()) {
                blockSourceIndexById.put(id, i);
            }
        }
    }

    private void refreshSelectedCell() {
        if (tree.getRoot() != null) {
            tree.refresh();
        }
    }

    private void queueSelectionRefresh(String blockId) {
        pendingSelectionBlockId = blockId == null ? "" : blockId;
        if (selectionUpdateQueued) {
            return;
        }
        selectionUpdateQueued = true;
        Platform.runLater(() -> {
            selectionUpdateQueued = false;
            selectNearestIndexEntry(pendingSelectionBlockId);
        });
    }

    private void selectNearestIndexEntry(String selectedBlockId) {
        if (tree.getRoot() == null) {
            return;
        }
        String normalizedSelectedBlockId = normalizeBlockId(selectedBlockId);
        String targetBlockId = nearestIndexedBlockId(normalizedSelectedBlockId);
        if (targetBlockId.isBlank()) {
            clearIndexSelectionIfNeeded(normalizedSelectedBlockId);
            return;
        }
        TreeItem<IndexEntry> target = indexedItemsByBlockId.get(targetBlockId);
        if (target == null) {
            return;
        }
        boolean targetChanged = !targetBlockId.equals(lastSelectedIndexBlockId);
        boolean styledSelectionChanged = !normalizedSelectedBlockId.equals(lastStyledSelectionBlockId);
        if (targetChanged) {
            syncingSelection = true;
            try {
                tree.getSelectionModel().select(target);
            } finally {
                syncingSelection = false;
            }
            lastSelectedIndexBlockId = targetBlockId;
        }
        if (targetChanged && !targetBlockId.equals(lastScrolledIndexBlockId)) {
            int row = tree.getRow(target);
            if (row >= 0) {
                tree.scrollTo(Math.max(0, row - 2));
            }
            lastScrolledIndexBlockId = targetBlockId;
        }
        if (styledSelectionChanged && (indexedItemsByBlockId.containsKey(normalizedSelectedBlockId)
                || indexedItemsByBlockId.containsKey(lastStyledSelectionBlockId))) {
            refreshSelectedCell();
        }
        lastStyledSelectionBlockId = normalizedSelectedBlockId;
    }

    private String nearestIndexedBlockId(String selectedBlockId) {
        String normalized = normalizeBlockId(selectedBlockId);
        if (normalized.isBlank()) {
            return "";
        }
        if (indexedItemsByBlockId.containsKey(normalized)) {
            return normalized;
        }
        Integer selectedIndex = blockSourceIndexById.get(normalized);
        if (selectedIndex == null || indexedBlocks.isEmpty()) {
            return "";
        }
        String lastIndexed = "";
        for (IndexedBlock indexedBlock : indexedBlocks) {
            if (indexedBlock.sourceIndex() > selectedIndex) {
                break;
            }
            lastIndexed = indexedBlock.blockId();
        }
        if (!lastIndexed.isBlank()) {
            return lastIndexed;
        }
        return indexedBlocks.get(0).blockId();
    }

    private void clearIndexSelectionIfNeeded(String normalizedSelectedBlockId) {
        if (!lastSelectedIndexBlockId.isBlank() || tree.getSelectionModel().getSelectedItem() != null) {
            syncingSelection = true;
            try {
                tree.getSelectionModel().clearSelection();
            } finally {
                syncingSelection = false;
            }
        }
        if (indexedItemsByBlockId.containsKey(lastStyledSelectionBlockId)) {
            refreshSelectedCell();
        }
        lastSelectedIndexBlockId = "";
        lastScrolledIndexBlockId = "";
        lastStyledSelectionBlockId = normalizedSelectedBlockId;
    }

    private static String normalizeBlockId(String blockId) {
        return blockId == null ? "" : blockId.strip();
    }

    private record IndexedBlock(String blockId, int sourceIndex) {
    }

    private record DocumentBookmarkEntry(String name, int page) {
        @Override
        public String toString() {
            String label = name == null || name.isBlank() ? "Pagina " + page : name;
            return label + " - p. " + page;
        }
    }

    private record IndexEntry(String blockId, DocumentBlockType kind, String label, PdfVisualTextHighlight highlight) {
        static IndexEntry root() {
            return new IndexEntry("", DocumentBlockType.EMPTY, "Documento", null);
        }

        static IndexEntry from(DocumentOutlineEntry entry) {
            return new IndexEntry(entry.blockId(), entry.kind(), entry.label(), null);
        }

        static IndexEntry message(String title, String detail) {
            return new IndexEntry("", DocumentBlockType.EMPTY, title + " - " + detail, null);
        }

        static IndexEntry searchResult(PdfTextSearchResult result) {
            String label = "p. " + result.pageNumber() + " - " + result.snippet();
            return new IndexEntry(result.regionId(), DocumentBlockType.PARAGRAPH, label, result.toHighlight());
        }
    }
}

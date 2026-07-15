package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import com.marcosmoreiradev.docupodcaststudio.application.document.BuildDocumentOutlineUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.document.BuildPdfEnhancedOutlineUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.document.DocumentOutlineEntry;
import com.marcosmoreiradev.docupodcaststudio.application.document.DocumentOutlineProjection;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfTextSearchProjection;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfTextSearchRequest;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfTextSearchResult;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfVisualTextHighlight;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfNarratableDocumentRequest;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfNarratableDocumentResolution;
import com.marcosmoreiradev.docupodcaststudio.application.document.ResolvePdfNarratableDocumentUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.document.SearchPdfTextUseCase;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlockType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentFormat;
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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

/** Navigable document outline rendered from an application projection without altering the source document. */
public final class DocumentIndexPanel extends BorderPane {
    private final ObservableValue<ReadableDocument> documentProperty;
    private final ObservableValue<String> selectedBlockIdProperty;
    private final Consumer<String> onBlockSelected;
    private final Consumer<PdfVisualTextHighlight> onPdfHighlightSelected;
    private final IntConsumer onPdfPageSelected;
    private final BuildDocumentOutlineUseCase buildOutline;
    private final BuildPdfEnhancedOutlineUseCase buildEnhancedOutline;
    private final SearchPdfTextUseCase searchPdfText;
    private final ResolvePdfNarratableDocumentUseCase resolvePdfNarratableDocument;
    private final Consumer<ReadableDocument> onNarratableDocumentResolved;
    private final Supplier<Path> ocrCacheDirectory;
    private final VBox header = new VBox(4);
    private final TextField pdfSearch = new TextField();
    private final TextField pdfPageJump = new TextField();
    private final Button nativeSearchButton = new Button("Buscar");
    private final Button ocrSearchButton = new Button("Mejorar busqueda");
    private final Button clearSearchButton = new Button("Limpiar");
    private final Button pdfPageJumpButton = new Button("Ir a pagina");
    private final ProgressIndicator searchProgress = new ProgressIndicator();
    private final Label searchStatus = note("");
    private final Label pdfPageJumpStatus = note("");
    private final HBox searchActions = new HBox(6);
    private final HBox pageJumpActions = new HBox(6);
    private final VBox searchBox = new VBox(6);
    private final VBox pageJumpBox = new VBox(6);
    private final TextField bookmarkName = new TextField();
    private final TextField bookmarkPage = new TextField();
    private final Button addBookmarkButton = new Button("Agregar marcador");
    private final Button goBookmarkButton = new Button("Ir");
    private final Button deleteBookmarkButton = new Button("Eliminar");
    private final ComboBox<DocumentBookmarkEntry> bookmarkChoice = new ComboBox<>();
    private final VBox bookmarksBox = new VBox(6);
    private final TreeView<IndexEntry> tree = new TreeView<>();
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
                null, null, null, null, null, null, null);
    }

    public DocumentIndexPanel(ObservableValue<ReadableDocument> documentProperty,
                              ObservableValue<String> selectedBlockIdProperty,
                              Consumer<String> onBlockSelected,
                              BuildDocumentOutlineUseCase buildOutline,
                              BuildPdfEnhancedOutlineUseCase buildEnhancedOutline,
                              SearchPdfTextUseCase searchPdfText,
                              Consumer<PdfVisualTextHighlight> onPdfHighlightSelected,
                              IntConsumer onPdfPageSelected,
                              Supplier<Path> ocrCacheDirectory,
                              ResolvePdfNarratableDocumentUseCase resolvePdfNarratableDocument,
                              Consumer<ReadableDocument> onNarratableDocumentResolved) {
        this.documentProperty = Objects.requireNonNull(documentProperty, "documentProperty");
        this.selectedBlockIdProperty = selectedBlockIdProperty;
        this.onBlockSelected = Objects.requireNonNull(onBlockSelected, "onBlockSelected");
        this.buildOutline = Objects.requireNonNull(buildOutline, "buildOutline");
        this.buildEnhancedOutline = buildEnhancedOutline;
        this.searchPdfText = searchPdfText;
        this.resolvePdfNarratableDocument = resolvePdfNarratableDocument;
        this.onNarratableDocumentResolved = onNarratableDocumentResolved;
        this.onPdfHighlightSelected = onPdfHighlightSelected;
        this.onPdfPageSelected = onPdfPageSelected;
        this.ocrCacheDirectory = ocrCacheDirectory == null ? () -> null : ocrCacheDirectory;
        getStyleClass().add("document-index-panel");
        header.getStyleClass().add("document-index-header");
        pdfSearch.getStyleClass().add("document-index-search-field");
        nativeSearchButton.getStyleClass().add("document-index-search-button");
        ocrSearchButton.getStyleClass().add("document-index-search-button");
        clearSearchButton.getStyleClass().add("document-index-search-button");
        pdfPageJump.getStyleClass().add("document-index-search-field");
        pdfPageJumpButton.getStyleClass().add("document-index-search-button");
        bookmarkName.getStyleClass().add("document-index-search-field");
        bookmarkPage.getStyleClass().add("document-index-search-field");
        bookmarkChoice.getStyleClass().add("document-index-search-field");
        addBookmarkButton.getStyleClass().add("document-index-search-button");
        goBookmarkButton.getStyleClass().add("document-index-search-button");
        deleteBookmarkButton.getStyleClass().add("document-index-search-button");
        Tooltip.install(ocrSearchButton, new Tooltip("Intenta detectar texto en paginas donde la busqueda normal no alcance."));
        Tooltip.install(nativeSearchButton, new Tooltip("Buscar en el texto ya disponible del PDF."));
        Tooltip.install(clearSearchButton, new Tooltip("Volver al temario o a las paginas del PDF."));
        Tooltip.install(pdfPageJumpButton, new Tooltip("Saltar directamente a una pagina del PDF."));
        Tooltip.install(addBookmarkButton, new Tooltip("Guardar una pagina con un nombre corto para volver despues."));
        Tooltip.install(goBookmarkButton, new Tooltip("Saltar al marcador seleccionado."));
        Tooltip.install(deleteBookmarkButton, new Tooltip("Quitar el marcador seleccionado de esta sesion."));
        searchProgress.setMaxSize(18, 18);
        searchProgress.setVisible(false);
        searchProgress.setManaged(false);
        searchActions.getStyleClass().add("document-index-search-actions");
        searchActions.getChildren().setAll(nativeSearchButton, ocrSearchButton, clearSearchButton, searchProgress);
        searchBox.getStyleClass().add("document-index-search");
        searchBox.getChildren().setAll(pdfSearch, searchActions, searchStatus);
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
        nativeSearchButton.setOnAction(event -> runPdfSearch(false));
        ocrSearchButton.setOnAction(event -> runPdfSearch(true));
        clearSearchButton.setOnAction(event -> render(documentProperty.getValue()));
        pdfPageJumpButton.setOnAction(event -> goToPdfPage());
        pdfSearch.setOnAction(event -> runPdfSearch(false));
        pdfPageJump.setOnAction(event -> goToPdfPage());
        addBookmarkButton.setOnAction(event -> addBookmark());
        goBookmarkButton.setOnAction(event -> goToBookmark());
        deleteBookmarkButton.setOnAction(event -> deleteBookmark());
        setTop(header);
        setCenter(tree);
        documentProperty.addListener((obs, oldValue, newValue) -> render(newValue));
        if (selectedBlockIdProperty != null) {
            selectedBlockIdProperty.addListener((obs, oldValue, newValue) -> queueSelectionRefresh(newValue));
        }
        render(documentProperty.getValue());
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
        if (pdfSearchAvailable(document)) {
            header.getChildren().add(pdfSearchControls());
        }
        if (pdfPagesFallback(document, projection)) {
            header.getChildren().add(pdfPageJumpControls(projection));
        }
        if (document != null) {
            header.getChildren().add(bookmarkControls(document));
        }
        TreeItem<IndexEntry> root = outlineRoot(projection);
        tree.setRoot(root);
        expandFirstLevel(root);
        queueSelectionRefresh(currentSelectedBlockId());
        maybeEnhancePdfOutline(document, projection);
    }

    private static Label note(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.getStyleClass().add("document-side-muted");
        return label;
    }

    private boolean pdfSearchAvailable(ReadableDocument document) {
        return document != null && document.format() == SourceDocumentFormat.PDF && searchPdfText != null;
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
        if (document != null && document.format() == SourceDocumentFormat.PDF
                && (bookmarkPage.getText() == null || bookmarkPage.getText().isBlank())) {
            bookmarkPage.setText(pdfPageJump.getText() == null || pdfPageJump.getText().isBlank()
                    ? "1"
                    : pdfPageJump.getText().strip());
        }
        return bookmarksBox;
    }

    private static boolean pdfPagesFallback(ReadableDocument document, DocumentOutlineProjection projection) {
        return document != null
                && document.format() == SourceDocumentFormat.PDF
                && projection != null
                && projection.origin() == com.marcosmoreiradev.docupodcaststudio.application.document.DocumentOutlineOrigin.PDF_PAGES;
    }

    private void maybeEnhancePdfOutline(ReadableDocument document, DocumentOutlineProjection baseProjection) {
        if (document == null || document.format() != SourceDocumentFormat.PDF
                || buildEnhancedOutline == null
                || strongProjection(baseProjection)) {
            return;
        }
        Task<DocumentOutlineProjection> task = new Task<>() {
            @Override
            protected DocumentOutlineProjection call() {
                return buildEnhancedOutline.build(document, ocrCacheDirectory.get());
            }
        };
        task.setOnSucceeded(event -> {
            if (document != documentProperty.getValue()) {
                return;
            }
            DocumentOutlineProjection enhanced = task.getValue();
            if (betterProjection(enhanced, baseProjection)) {
                applyProjection(enhanced);
            }
        });
        Thread worker = new Thread(task, "pdf-outline-enhance");
        worker.setDaemon(true);
        worker.start();
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
        if (pdfSearchAvailable(documentProperty.getValue())) {
            header.getChildren().add(pdfSearchControls());
        }
        if (pdfPagesFallback(documentProperty.getValue(), projection)) {
            header.getChildren().add(pdfPageJumpControls(projection));
        }
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

    private void runPdfSearch(boolean includeOcr) {
        ReadableDocument document = documentProperty.getValue();
        if (!pdfSearchAvailable(document)) {
            return;
        }
        String query = pdfSearch.getText() == null ? "" : pdfSearch.getText().strip();
        if (query.isBlank()) {
            searchStatus.setText("Escribe texto para buscar en el PDF.");
            return;
        }
        setSearchBusy(true, includeOcr ? "Mejorando busqueda..." : "Buscando en el PDF...");
        Task<PdfSearchOutcome> task = new Task<>() {
            @Override
            protected PdfSearchOutcome call() {
                Path cache = ocrCacheDirectory.get();
                PdfNarratableDocumentResolution resolution = null;
                ReadableDocument searchable = document;
                if (includeOcr && resolvePdfNarratableDocument != null) {
                    resolution = resolvePdfNarratableDocument.resolve(new PdfNarratableDocumentRequest(document, cache, false, 24));
                    searchable = resolution.document() == null ? document : resolution.document();
                }
                PdfTextSearchProjection projection = searchPdfText.search(new PdfTextSearchRequest(
                        searchable,
                        query,
                        includeOcr && resolvePdfNarratableDocument == null,
                        80,
                        24,
                        cache));
                return new PdfSearchOutcome(projection, resolution);
            }
        };
        task.setOnSucceeded(event -> {
            if (document != documentProperty.getValue()) {
                return;
            }
            setSearchBusy(false, "");
            PdfSearchOutcome outcome = task.getValue();
            if (outcome != null && outcome.resolution() != null && outcome.resolution().changed()
                    && outcome.resolution().document() != null && onNarratableDocumentResolved != null) {
                onNarratableDocumentResolved.accept(outcome.resolution().document());
            }
            showSearchResults(outcome == null ? null : outcome.projection(), includeOcr);
        });
        task.setOnFailed(event -> setSearchBusy(false,
                "No se pudo buscar en PDF: " + diagnostic(task.getException())));
        Thread worker = new Thread(task, includeOcr ? "pdf-search-ocr" : "pdf-search-native");
        worker.setDaemon(true);
        worker.start();
    }

    private void setSearchBusy(boolean busy, String status) {
        searchProgress.setVisible(busy);
        searchProgress.setManaged(busy);
        nativeSearchButton.setDisable(busy);
        ocrSearchButton.setDisable(busy);
        clearSearchButton.setDisable(busy);
        searchStatus.setText(status == null ? "" : status);
    }

    private void showSearchResults(PdfTextSearchProjection projection, boolean includeOcr) {
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
                    includeOcr ? "No se encontro texto despues de mejorar la busqueda." : "No se encontro texto. Prueba Mejorar busqueda.")));
        }
        tree.setRoot(root);
        expandFirstLevel(root);
        String improved = includeOcr ? " con busqueda mejorada" : "";
        searchStatus.setText(projection.results().size() + " resultado(s)" + improved
                + " en " + projection.scannedPages() + " pagina(s).");
    }

    private static String purposeText(ReadableDocument document, DocumentOutlineProjection projection) {
        if (document != null && document.format() == SourceDocumentFormat.PDF && projection != null) {
            if (projection.origin() == com.marcosmoreiradev.docupodcaststudio.application.document.DocumentOutlineOrigin.PDF_PAGES) {
                return "Paginas del PDF.";
            }
            return "Temario del PDF.";
        }
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

    private record PdfSearchOutcome(PdfTextSearchProjection projection, PdfNarratableDocumentResolution resolution) {
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
            return new IndexEntry(result.blockId(), DocumentBlockType.PARAGRAPH, label, result.toHighlight());
        }
    }
}

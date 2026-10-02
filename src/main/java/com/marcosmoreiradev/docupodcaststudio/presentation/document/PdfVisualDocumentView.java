package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioViewportControls;

import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioFeedbackControls;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioNavigationControls;

import com.marcosmoreiradev.docupodcaststudio.application.document.BuildPdfVisualDocumentUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfPageRenderRequest;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfPageRenderResult;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfPageCoordinateTransform;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfPageRegion;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfRenderException;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfVisualReadingProjection;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfVisualTextHighlight;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfVisualTextTarget;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfTextLayerOrigin;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfPlaybackVisualGuard;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfViewportSelection;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfVisualDocument;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfVisualPage;
import com.marcosmoreiradev.docupodcaststudio.application.document.RenderPdfVisualPageUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.document.PreparedPdfWorkspaceRef;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfNarrationFocusRef;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.concurrent.Task;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.nio.IntBuffer;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.function.IntSupplier;

/** Visual PDF reader surface used only when the source document is a PDF. */
public final class PdfVisualDocumentView extends BorderPane {
    static final String TEXT_PADDING_HORIZONTAL_PROPERTY =
            "docupodcast.pdf.playback.highlight.padding.horizontal.px";
    static final String TEXT_PADDING_VERTICAL_PROPERTY =
            "docupodcast.pdf.playback.highlight.padding.vertical.px";
    static final double DEFAULT_TEXT_PADDING_HORIZONTAL_PX = 12.0;
    static final double DEFAULT_TEXT_PADDING_VERTICAL_PX = 25.0;
    private static final Logger LOGGER = LoggerFactory.getLogger(PdfVisualDocumentView.class);
    public static final boolean PDF_RENDER_DIAGNOSTICS = false;

    private static final int PAGE_RENDER_CACHE_LIMIT = 8;
    private static final int PRELOAD_BEFORE = 1;
    private static final int PRELOAD_AFTER = 1;
    private static final int UNLOAD_DISTANCE = 5;
    private static final int PDF_RENDER_POOL_SIZE = 2;
    private static final int MAX_RENDER_QUEUE_SIZE = 10;
    private static final int MIN_DPI = 144;
    private static final int MAX_DPI = 288;
    private static final double SHARP_RENDER_PIXEL_MARGIN = 1.35;
    private static final long MAX_PAGE_PIXEL_COUNT = 48_000_000L;
    private static final double MIN_REGION_SELECTION_SIZE = 10.0;

    private final BuildPdfVisualDocumentUseCase buildPdfVisualDocument;
    private final RenderPdfVisualPageUseCase renderPdfVisualPage;
    private final IntSupplier zoomPercentSupplier;
    private final Consumer<PdfViewportSelection> regionSelectionHandler;
    private final ScrollPane scroll = StudioViewportControls.scrollPane();
    private final VBox pages = new VBox(18);
    private final StackPane pagesHost = new StackPane(pages);
    private final Pane regionCaptureLayer = new Pane();
    private final Rectangle globalRegionSelectionRectangle = new Rectangle();
    private final DoubleProperty zoomScale = new SimpleDoubleProperty(1.0);
    private final ReadOnlyDoubleWrapper scrollProgress = new ReadOnlyDoubleWrapper(0.0);
    private final ReadOnlyIntegerWrapper visiblePageNumber = new ReadOnlyIntegerWrapper(0);
    private final Map<Integer, PdfPageSlot> slots = new HashMap<>();
    private final Set<Integer> renderingPages = new HashSet<>();
    private final Set<Integer> queuedPages = new HashSet<>();
    private final Deque<Integer> renderQueue = new ArrayDeque<>();
    private final LinkedHashMap<PageKey, Image> pageCache = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<PageKey, Image> eldest) {
            return size() > PAGE_RENDER_CACHE_LIMIT;
        }
    };

    private Path currentSourcePath;
    private PdfVisualDocument visualDocument;
    private boolean regionSelectionActive;
    private PdfVisualTextHighlight activeTextHighlight;
    private final PdfPlaybackVisualGuard playbackVisualGuard = new PdfPlaybackVisualGuard();
    private long activePlaybackPaintSequence;
    private PdfNarrationFocusRef activeNarrationFocus;
    private PdfVisualReadingProjection readingProjection = new PdfVisualReadingProjection(List.of(), Map.of(), List.of());
    private PdfVisualTextTarget hoverTextTarget;
    private PdfVisualTextTarget fixedTextTarget;
    private PendingTextTargetRequest pendingTextTargetRequest;
    private Consumer<PdfVisualTextTarget> textTargetSelectionHandler;
    private Consumer<PdfVisualTextTarget> contentDescriptionRequestHandler;
    private Consumer<PdfVisualTextTarget> manualDescriptionRequestHandler;
    private Consumer<PdfVisualTextTarget> narrateFromTargetRequestHandler;
    private Runnable emptyTextTargetSelectionHandler;
    private ContextMenu activeTextTargetContextMenu;
    private Consumer<Integer> textPreparationRequestHandler;
    private PdfPageSlot activeRegionSelectionSlot;
    private double globalSelectionStartImageX;
    private double globalSelectionStartImageY;
    private int activeRenderTasks;
    private long renderRevision;

    public PdfVisualDocumentView(BuildPdfVisualDocumentUseCase buildPdfVisualDocument,
                                 RenderPdfVisualPageUseCase renderPdfVisualPage,
                                 IntSupplier zoomPercentSupplier) {
        this(buildPdfVisualDocument, renderPdfVisualPage, zoomPercentSupplier, null);
    }

    public PdfVisualDocumentView(BuildPdfVisualDocumentUseCase buildPdfVisualDocument,
                                 RenderPdfVisualPageUseCase renderPdfVisualPage,
                                 IntSupplier zoomPercentSupplier,
                                 Consumer<PdfViewportSelection> regionSelectionHandler) {
        this.buildPdfVisualDocument = buildPdfVisualDocument;
        this.renderPdfVisualPage = renderPdfVisualPage;
        this.zoomPercentSupplier = zoomPercentSupplier == null ? () -> 100 : zoomPercentSupplier;
        this.regionSelectionHandler = regionSelectionHandler;

        getStyleClass().add("pdf-visual-workspace");
        setFocusTraversable(true);
        addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() != KeyCode.ESCAPE) return;
            dismissTextTargetContextMenu();
            clearPinnedTextTarget();
            clearHoverTextTarget();
            if (emptyTextTargetSelectionHandler != null) {
                emptyTextTargetSelectionHandler.run();
            }
            event.consume();
        });
        pagesHost.getStyleClass().add("pdf-visual-pages-host");
        pages.getStyleClass().add("pdf-visual-pages");
        pages.setAlignment(Pos.TOP_CENTER);
        pages.setPadding(new Insets(126, 18, 82, 18));
        pagesHost.setAlignment(Pos.TOP_CENTER);
        pagesHost.minWidthProperty().bind(Bindings.createDoubleBinding(
                () -> Math.max(0.0, viewportWidth()),
                scroll.viewportBoundsProperty(),
                widthProperty()));
        scroll.setContent(pagesHost);
        scroll.setFitToWidth(false);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.getStyleClass().add("pdf-visual-scroll");
        regionCaptureLayer.getStyleClass().add("pdf-visual-global-region-overlay");
        regionCaptureLayer.setPickOnBounds(true);
        regionCaptureLayer.setMouseTransparent(true);
        regionCaptureLayer.setVisible(false);
        regionCaptureLayer.setMinSize(0, 0);
        regionCaptureLayer.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        globalRegionSelectionRectangle.getStyleClass().add("pdf-visual-region-selection");
        globalRegionSelectionRectangle.setManaged(false);
        globalRegionSelectionRectangle.setMouseTransparent(true);
        globalRegionSelectionRectangle.setVisible(false);
        regionCaptureLayer.getChildren().add(globalRegionSelectionRectangle);
        installGlobalRegionSelectionHandlers();
        StackPane visualHost = new StackPane(scroll, regionCaptureLayer);
        regionCaptureLayer.prefWidthProperty().bind(visualHost.widthProperty());
        regionCaptureLayer.prefHeightProperty().bind(visualHost.heightProperty());
        visualHost.getStyleClass().add("pdf-visual-host");
        setCenter(visualHost);

        InvalidationListener visiblePageRefresh = ignored -> renderVisiblePages();
        scroll.vvalueProperty().addListener(visiblePageRefresh);
        scroll.vvalueProperty().addListener((obs, oldValue, newValue) -> updateScrollProgress());
        scroll.viewportBoundsProperty().addListener(visiblePageRefresh);
        widthProperty().addListener(visiblePageRefresh);
        refreshZoom();
    }

    public ReadOnlyDoubleProperty documentProgressProperty() {
        return scrollProgress.getReadOnlyProperty();
    }

    public ReadOnlyIntegerProperty visiblePageNumberProperty() {
        return visiblePageNumber.getReadOnlyProperty();
    }

    public void setRegionSelectionActive(boolean active) {
        regionSelectionActive = active;
        if (active) {
            clearHoverTextTarget();
        }
        slots.values().forEach(PdfPageSlot::updateRegionSelectionState);
        updateGlobalRegionSelectionState();
    }

    public void setReadingProjection(PdfVisualReadingProjection projection) {
        readingProjection = projection == null ? new PdfVisualReadingProjection(List.of(), Map.of(), List.of()) : projection;
        hoverTextTarget = null;
        fixedTextTarget = null;
        slots.values().forEach(PdfPageSlot::updateTextTargetOverlays);
        retryPendingTextTargetSelection();
    }

    public void setTextTargetSelectionHandler(Consumer<PdfVisualTextTarget> handler) {
        textTargetSelectionHandler = handler;
    }

    public void setManualDescriptionRequestHandler(
            Consumer<PdfVisualTextTarget> handler) {
        manualDescriptionRequestHandler = handler;
    }

    public void setContentDescriptionRequestHandler(
            Consumer<PdfVisualTextTarget> handler) {
        contentDescriptionRequestHandler = handler;
    }

    public void setNarrateFromTargetRequestHandler(
            Consumer<PdfVisualTextTarget> handler) {
        narrateFromTargetRequestHandler = handler;
    }

    public void setEmptyTextTargetSelectionHandler(Runnable handler) {
        emptyTextTargetSelectionHandler = handler;
    }

    public void setTextPreparationRequestHandler(Consumer<Integer> handler) {
        textPreparationRequestHandler = handler;
    }

    public void showDocument(PreparedPdfWorkspaceRef workspace) {
        currentSourcePath = workspace == null ? null : workspace.sourcePath();
        visualDocument = null;
        activeTextHighlight = null;
        playbackVisualGuard.clear();
        activeNarrationFocus = null;
        hoverTextTarget = null;
        fixedTextTarget = null;
        pendingTextTargetRequest = null;
        pageCache.clear();
        clearPendingRenders();
        slots.clear();
        pages.getChildren().clear();
        visiblePageNumber.set(0);
        scroll.setVvalue(0.0);
        scroll.setHvalue(0.0);
        updateScrollProgress();
        if (workspace == null) {
            pages.getChildren().add(message("Sin PDF activo."));
            return;
        }
        try {
            visualDocument = buildPdfVisualDocument.build(workspace.sourcePath(), dpiForZoom());
            if (visualDocument.pageCount() <= 0 || visualDocument.pages().isEmpty()) {
                pages.getChildren().add(message("El PDF no reporta paginas renderizables."));
                return;
            }
            for (PdfVisualPage page : visualDocument.pages()) {
                PdfPageSlot slot = new PdfPageSlot(page);
                slots.put(page.pageNumber(), slot);
                pages.getChildren().add(slot.root());
            }
            Platform.runLater(this::renderVisiblePages);
        } catch (PdfRenderException ex) {
            pages.getChildren().add(errorMessage("No se pudo inspeccionar el PDF.", ex));
        } catch (RuntimeException ex) {
            pages.getChildren().add(errorMessage("No se pudo preparar el visor PDF.", ex));
        }
    }

    public void clear() {
        currentSourcePath = null;
        visualDocument = null;
        regionSelectionActive = false;
        activeRegionSelectionSlot = null;
        globalRegionSelectionRectangle.setVisible(false);
        updateGlobalRegionSelectionState();
        activeTextHighlight = null;
        playbackVisualGuard.clear();
        activeNarrationFocus = null;
        hoverTextTarget = null;
        fixedTextTarget = null;
        pendingTextTargetRequest = null;
        readingProjection = new PdfVisualReadingProjection(List.of(), Map.of(), List.of());
        pageCache.clear();
        clearPendingRenders();
        slots.clear();
        pages.getChildren().clear();
        visiblePageNumber.set(0);
        updateScrollProgress();
    }

    public void refreshZoom() {
        double next = Math.max(0.75, Math.min(2.25, zoomPercentSupplier.getAsInt() / 100.0));
        if (Math.abs(next - zoomScale.get()) < 0.01) {
            return;
        }
        zoomScale.set(next);
        pageCache.clear();
        clearPendingRenders();
        slots.values().forEach(PdfPageSlot::reset);
        Platform.runLater(this::renderVisiblePages);
    }

    public void showTextHighlight(PdfVisualTextHighlight highlight) {
        playbackVisualGuard.clear();
        applyTextHighlight(highlight);
    }

    public void showPlaybackTextTarget(PdfVisualTextTarget target, long sequence) {
        if (target == null || !target.available()
                || !playbackVisualGuard.request(sequence, target.regionId())) return;
        activePlaybackPaintSequence = sequence;
        applyTextHighlight(target.highlight());
    }

    private void applyTextHighlight(PdfVisualTextHighlight highlight) {
        activeTextHighlight = highlight == null || !highlight.available() ? null : highlight;
        slots.values().forEach(PdfPageSlot::updateTextHighlight);
        if (activeTextHighlight != null) {
            renderPage(activeTextHighlight.pageNumber());
        }
    }

    public void clearTextHighlight() {
        activeTextHighlight = null;
        playbackVisualGuard.clear();
        slots.values().forEach(PdfPageSlot::updateTextHighlight);
    }

    PdfPlaybackVisualGuard.Snapshot playbackVisualSnapshot() {
        return playbackVisualGuard.snapshot();
    }

    /**
     * Keeps non-text content framed until the playback coordinator explicitly
     * changes or clears the cue. Pausing therefore does not make it disappear.
     */
    public void showNarrationFocus(PdfNarrationFocusRef focus) {
        activeNarrationFocus = focus;
        slots.values().forEach(PdfPageSlot::updateNarrationFocus);
        if (focus != null && !focus.boxes().isEmpty()) {
            renderPage(focus.pageNumber());
            PdfNarrationFocusRef.FocusBox box = focus.boxes().getFirst();
            PdfPageSlot slot = slots.get(focus.pageNumber());
            if (slot != null) {
                scrollToPageRegion(focus.pageNumber(), new PdfPageRegion(
                        focus.pageNumber(), box.xMin(), box.yMin(), box.xMax(), box.yMax(),
                        slot.page.widthPoints(), slot.page.heightPoints()), 72.0);
            }
        }
    }

    public void clearNarrationFocus() {
        activeNarrationFocus = null;
        slots.values().forEach(PdfPageSlot::updateNarrationFocus);
    }

    public PdfNarrationFocusRef narrationFocus() {
        return activeNarrationFocus;
    }

    public void showPinnedTextTarget(PdfVisualTextTarget target) {
        fixedTextTarget = target == null || !target.available() ? null : target;
        slots.values().forEach(PdfPageSlot::updateFixedTextTarget);
        if (fixedTextTarget != null) {
            renderPage(fixedTextTarget.pageNumber());
        }
    }

    public void clearPinnedTextTarget() {
        dismissTextTargetContextMenu();
        fixedTextTarget = null;
        slots.values().forEach(PdfPageSlot::updateFixedTextTarget);
    }

    private void showHoverTextTarget(PdfVisualTextTarget target) {
        PdfVisualTextTarget next = target == null || !target.available() ? null : target;
        if (java.util.Objects.equals(hoverTextTarget, next)) {
            return;
        }
        hoverTextTarget = next;
        slots.values().forEach(PdfPageSlot::updateHoverTextTarget);
    }

    private void clearHoverTextTarget() {
        showHoverTextTarget(null);
    }

    private void retryPendingTextTargetSelection() {
        PendingTextTargetRequest request = pendingTextTargetRequest;
        if (request == null || readingProjection == null) {
            return;
        }
        PdfVisualTextTarget target = readingProjection.targetAt(request.pageNumber(), request.xPoints(), request.yPoints()).orElse(null);
        if (target != null && target.available()) {
            pendingTextTargetRequest = null;
            showPinnedTextTarget(target);
            if (textTargetSelectionHandler != null) {
                textTargetSelectionHandler.accept(target);
            }
            return;
        }
        boolean pageHasTargets = readingProjection.targets().stream().anyMatch(candidate -> candidate.pageNumber() == request.pageNumber());
        if (pageHasTargets) {
            pendingTextTargetRequest = null;
        }
    }

    public void scrollToPage(int pageNumber) {
        if (visualDocument == null || visualDocument.pageCount() <= 0) {
            return;
        }
        int page = Math.max(1, Math.min(visualDocument.pageCount(), pageNumber));
        visiblePageNumber.set(page);
        renderPage(page);
        Platform.runLater(() -> {
            if (visualDocument == null || visualDocument.pageCount() <= 1) {
                scroll.setVvalue(0);
                return;
            }
            scroll.setVvalue((double) (page - 1) / Math.max(1, visualDocument.pageCount() - 1));
        });
    }

    public void scrollToTextTarget(PdfVisualTextTarget target, double topOffsetPx) {
        if (target == null || !target.available()) {
            return;
        }
        scrollToPageRegion(target.pageNumber(), target.region(), topOffsetPx);
    }

    public void scrollToTextHighlight(PdfVisualTextHighlight highlight, double topOffsetPx) {
        if (highlight == null || !highlight.available()) {
            return;
        }
        scrollToPageRegion(highlight.pageNumber(), highlight.region(), topOffsetPx);
    }

    private void scrollToPageRegion(int pageNumber, PdfPageRegion region, double topOffsetPx) {
        if (visualDocument == null || visualDocument.pageCount() <= 0 || region == null) {
            return;
        }
        int page = Math.max(1, Math.min(visualDocument.pageCount(), pageNumber));
        visiblePageNumber.set(page);
        renderPage(page);
        Platform.runLater(() -> {
            PdfPageSlot slot = slots.get(page);
            Point2D scenePoint = slot == null ? null : slot.regionTopScenePoint(region);
            if (scenePoint == null) {
                scrollToPage(page);
                return;
            }
            Bounds viewport = scroll.getViewportBounds();
            double viewportHeight = viewport == null ? 0.0 : viewport.getHeight();
            double contentHeight = pagesHost.getBoundsInLocal().getHeight();
            double scrollableHeight = Math.max(1.0, contentHeight - viewportHeight);
            Point2D hostPoint = pagesHost.sceneToLocal(scenePoint);
            double target = (hostPoint.getY() - Math.max(0.0, topOffsetPx)) / scrollableHeight;
            scroll.setVvalue(Math.max(0.0, Math.min(1.0, target)));
        });
    }

    private void renderVisiblePages() {
        if (visualDocument == null || visualDocument.pageCount() <= 0 || slots.isEmpty()) {
            return;
        }
        updateScrollProgress();
        int pageCount = visualDocument.pageCount();
        int center = pageCount == 1 ? 1 : (int) Math.round(scroll.getVvalue() * (pageCount - 1)) + 1;
        center = Math.max(1, Math.min(pageCount, center));
        visiblePageNumber.set(center);
        int start = Math.max(1, center - PRELOAD_BEFORE);
        int end = Math.min(pageCount, center + PRELOAD_AFTER);
        unloadDistantPages(center);
        for (int page = start; page <= end; page++) {
            renderPage(page);
        }
    }

    private void updateScrollProgress() {
        double progress = visualDocument == null || visualDocument.pageCount() <= 0
                ? 0.0
                : Math.max(0.0, Math.min(1.0, scroll.getVvalue()));
        scrollProgress.set(progress);
    }

    private void installGlobalRegionSelectionHandlers() {
        regionCaptureLayer.addEventFilter(MouseEvent.MOUSE_PRESSED, this::beginGlobalRegionSelection);
        regionCaptureLayer.addEventFilter(MouseEvent.MOUSE_DRAGGED, this::dragGlobalRegionSelection);
        regionCaptureLayer.addEventFilter(MouseEvent.MOUSE_RELEASED, this::finishGlobalRegionSelection);
    }

    private void updateGlobalRegionSelectionState() {
        boolean active = regionSelectionActive && regionSelectionHandler != null;
        regionCaptureLayer.setVisible(active);
        regionCaptureLayer.setMouseTransparent(!active);
        regionCaptureLayer.setCursor(active ? Cursor.CROSSHAIR : Cursor.DEFAULT);
        if (!active) {
            activeRegionSelectionSlot = null;
            globalRegionSelectionRectangle.setVisible(false);
        }
    }

    private void beginGlobalRegionSelection(MouseEvent event) {
        if (!regionSelectionActive || regionSelectionHandler == null) {
            return;
        }
        PdfPageSlot slot = slotAtScenePoint(event.getSceneX(), event.getSceneY());
        if (slot == null) {
            return;
        }
        Point2D imagePoint = slot.imagePointFromScene(event.getSceneX(), event.getSceneY(), false);
        if (imagePoint == null) {
            return;
        }
        activeRegionSelectionSlot = slot;
        globalSelectionStartImageX = imagePoint.getX();
        globalSelectionStartImageY = imagePoint.getY();
        updateGlobalSelectionRectangle(slot, imagePoint);
        event.consume();
    }

    private void dragGlobalRegionSelection(MouseEvent event) {
        if (activeRegionSelectionSlot == null || !globalRegionSelectionRectangle.isVisible()) {
            return;
        }
        Point2D imagePoint = activeRegionSelectionSlot.imagePointFromScene(event.getSceneX(), event.getSceneY(), true);
        if (imagePoint == null) {
            return;
        }
        updateGlobalSelectionRectangle(activeRegionSelectionSlot, imagePoint);
        event.consume();
    }

    private void finishGlobalRegionSelection(MouseEvent event) {
        PdfPageSlot slot = activeRegionSelectionSlot;
        if (slot == null || !globalRegionSelectionRectangle.isVisible()) {
            activeRegionSelectionSlot = null;
            return;
        }
        Point2D imagePoint = slot.imagePointFromScene(event.getSceneX(), event.getSceneY(), true);
        globalRegionSelectionRectangle.setVisible(false);
        activeRegionSelectionSlot = null;
        if (imagePoint == null) {
            event.consume();
            return;
        }
        double endX = imagePoint.getX();
        double endY = imagePoint.getY();
        if (Math.abs(endX - globalSelectionStartImageX) < MIN_REGION_SELECTION_SIZE
                || Math.abs(endY - globalSelectionStartImageY) < MIN_REGION_SELECTION_SIZE) {
            event.consume();
            return;
        }
        Image image = slot.image();
        if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
            event.consume();
            return;
        }
        PdfViewportSelection selection = new PdfViewportSelection(
                slot.page().pageNumber(),
                globalSelectionStartImageX,
                globalSelectionStartImageY,
                endX,
                endY,
                image.getWidth(),
                image.getHeight(),
                slot.page().widthPoints(),
                slot.page().heightPoints());
        regionSelectionHandler.accept(selection);
        event.consume();
    }

    private void updateGlobalSelectionRectangle(PdfPageSlot slot, Point2D endImagePoint) {
        Point2D startScene = slot.imagePixelToScenePoint(globalSelectionStartImageX, globalSelectionStartImageY);
        Point2D endScene = slot.imagePixelToScenePoint(endImagePoint.getX(), endImagePoint.getY());
        if (startScene == null || endScene == null) {
            globalRegionSelectionRectangle.setVisible(false);
            return;
        }
        Point2D start = regionCaptureLayer.sceneToLocal(startScene);
        Point2D end = regionCaptureLayer.sceneToLocal(endScene);
        globalRegionSelectionRectangle.setX(Math.min(start.getX(), end.getX()));
        globalRegionSelectionRectangle.setY(Math.min(start.getY(), end.getY()));
        globalRegionSelectionRectangle.setWidth(Math.abs(end.getX() - start.getX()));
        globalRegionSelectionRectangle.setHeight(Math.abs(end.getY() - start.getY()));
        globalRegionSelectionRectangle.setVisible(true);
        globalRegionSelectionRectangle.toFront();
    }

    private PdfPageSlot slotAtScenePoint(double sceneX, double sceneY) {
        PdfPageSlot fallback = null;
        double bestDistance = Double.MAX_VALUE;
        for (PdfPageSlot slot : slots.values()) {
            if (!slot.canSelectRegion()) {
                continue;
            }
            Bounds bounds = slot.displayedImageSceneBounds();
            if (bounds == null) {
                continue;
            }
            if (bounds.contains(sceneX, sceneY)) {
                return slot;
            }
            double centerY = (bounds.getMinY() + bounds.getMaxY()) / 2.0;
            double distance = Math.abs(sceneY - centerY);
            if (distance < bestDistance && sceneY >= bounds.getMinY() - 24 && sceneY <= bounds.getMaxY() + 24) {
                bestDistance = distance;
                fallback = slot;
            }
        }
        return fallback;
    }

    private void unloadDistantPages(int centerPage) {
        for (Map.Entry<Integer, PdfPageSlot> entry : slots.entrySet()) {
            if (Math.abs(entry.getKey() - centerPage) > UNLOAD_DISTANCE) {
                entry.getValue().unloadImage();
            }
        }
    }

    private void renderPage(int pageNumber) {
        PdfPageSlot slot = slots.get(pageNumber);
        Path sourcePath = currentSourcePath;
        PdfVisualDocument pdf = visualDocument;
        if (slot == null || sourcePath == null || pdf == null) {
            return;
        }
        int dpi = dpiForVisibleWidth(slot.page());
        PageKey key = new PageKey(sourcePath, pageNumber, dpi);
        Image cached = pageCache.get(key);
        if (cached != null) {
            slot.showImage(cached);
            return;
        }
        if (renderingPages.contains(pageNumber) || !queuedPages.add(pageNumber)) {
            return;
        }
        renderQueue.addLast(pageNumber);
        trimRenderQueue();
        slot.showLoading();
        reportPdfRenderDiagnostics("queued page " + pageNumber + " dpi=" + dpi);
        drainRenderQueue();
    }

    private void drainRenderQueue() {
        while (activeRenderTasks < PDF_RENDER_POOL_SIZE && !renderQueue.isEmpty()) {
            int pageNumber = renderQueue.removeFirst();
            queuedPages.remove(pageNumber);
            startRenderTask(pageNumber);
        }
    }

    private void startRenderTask(int pageNumber) {
        PdfPageSlot slot = slots.get(pageNumber);
        Path sourcePath = currentSourcePath;
        PdfVisualDocument pdf = visualDocument;
        if (slot == null || sourcePath == null || pdf == null) {
            return;
        }
        int dpi = dpiForVisibleWidth(slot.page());
        PageKey key = new PageKey(sourcePath, pageNumber, dpi);
        Image cached = pageCache.get(key);
        if (cached != null) {
            slot.showImage(cached);
            drainRenderQueue();
            return;
        }
        if (!renderingPages.add(pageNumber)) {
            drainRenderQueue();
            return;
        }
        long revision = renderRevision;
        activeRenderTasks++;
        reportPdfRenderDiagnostics("start page " + pageNumber + " dpi=" + dpi);
        slot.showLoading();
        Task<PdfPageRenderResult> task = new Task<>() {
            @Override
            protected PdfPageRenderResult call() throws Exception {
                return renderPdfVisualPage.render(new PdfPageRenderRequest(
                        sourcePath,
                        pageNumber,
                        dpi,
                        MAX_PAGE_PIXEL_COUNT,
                        Color.WHITE,
                        true));
            }
        };
        task.setOnSucceeded(event -> {
            renderingPages.remove(pageNumber);
            activeRenderTasks = Math.max(0, activeRenderTasks - 1);
            if (!sameDocument(sourcePath) || revision != renderRevision) {
                drainRenderQueue();
                return;
            }
            try {
                Image image = toFxImage(task.getValue().image());
                pageCache.put(key, image);
                slot.showImage(image);
                reportPdfRenderDiagnostics("done page " + pageNumber + " dpi=" + dpi);
            } catch (RuntimeException ex) {
                slot.showError("La pagina PDF " + pageNumber + " se renderizo, pero no pudo mostrarse: " + ex.getMessage());
                reportPdfRenderDiagnostics("display-error page " + pageNumber + " dpi=" + dpi);
            }
            drainRenderQueue();
        });
        task.setOnFailed(event -> {
            renderingPages.remove(pageNumber);
            activeRenderTasks = Math.max(0, activeRenderTasks - 1);
            if (!sameDocument(sourcePath) || revision != renderRevision) {
                drainRenderQueue();
                return;
            }
            slot.showError(pageError(pageNumber, task.getException()));
            reportPdfRenderDiagnostics("failed page " + pageNumber + " dpi=" + dpi);
            drainRenderQueue();
        });
        Thread worker = new Thread(task, "pdf-page-render-" + pageNumber);
        worker.setDaemon(true);
        worker.start();
    }

    private void clearPendingRenders() {
        renderRevision++;
        renderingPages.clear();
        queuedPages.clear();
        renderQueue.clear();
        reportPdfRenderDiagnostics("clear pending renders");
    }

    private void trimRenderQueue() {
        while (renderQueue.size() > MAX_RENDER_QUEUE_SIZE) {
            Integer removed = renderQueue.pollFirst();
            if (removed != null) {
                queuedPages.remove(removed);
                reportPdfRenderDiagnostics("trim queued page " + removed);
            }
        }
    }

    private void reportPdfRenderDiagnostics(String event) {
        if (!PDF_RENDER_DIAGNOSTICS) {
            return;
        }
        LOGGER.debug("PDF visual render: {} active={} queued={} rendering={} cache={}",
                event, activeRenderTasks, renderQueue.size(), renderingPages.size(), pageCache.size());
    }

    private boolean sameDocument(Path sourcePath) {
        return currentSourcePath != null && currentSourcePath.equals(sourcePath);
    }

    private int dpiForZoom() {
        return dpiForZoom(PdfPageRenderRequest.DEFAULT_DPI);
    }

    private int dpiForZoom(int recommendedDpi) {
        int base = recommendedDpi <= 0 ? PdfPageRenderRequest.DEFAULT_DPI : recommendedDpi;
        return Math.max(MIN_DPI, Math.min(MAX_DPI, (int) Math.round(base * zoomScale.get())));
    }

    private int dpiForVisibleWidth(PdfVisualPage page) {
        double widthPoints = page == null ? 612.0 : Math.max(1.0, page.widthPoints());
        double displayWidth = visiblePageWidthForZoom();
        double widthInches = widthPoints / 72.0;
        int required = (int) Math.ceil((displayWidth * SHARP_RENDER_PIXEL_MARGIN) / Math.max(1.0, widthInches));
        int recommended = page == null ? PdfPageRenderRequest.DEFAULT_DPI : page.recommendedDpi();
        return Math.max(MIN_DPI, Math.min(MAX_DPI, Math.max(required, recommended)));
    }

    private double visiblePageWidthForZoom() {
        return Math.max(320.0, viewportWidth() - 86.0) * zoomScale.get();
    }

    private static Image toFxImage(BufferedImage source) {
        WritableImage fx = new WritableImage(source.getWidth(), source.getHeight());
        PixelWriter writer = fx.getPixelWriter();
        PixelFormat<IntBuffer> format = PixelFormat.getIntArgbInstance();
        int width = source.getWidth();
        int[] row = new int[width];
        for (int y = 0; y < source.getHeight(); y++) {
            source.getRGB(0, y, width, 1, row, 0, width);
            writer.setPixels(0, y, width, 1, format, row, 0, width);
        }
        return fx;
    }

    private static Node message(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.getStyleClass().add("pdf-visual-message");
        return label;
    }

    private static Node errorMessage(String text, Throwable ex) {
        Label label = new Label(text + " " + diagnostic(ex));
        label.setWrapText(true);
        label.getStyleClass().add("pdf-visual-error");
        return label;
    }

    private static String pageError(int pageNumber, Throwable ex) {
        return "No se pudo renderizar la pagina PDF " + pageNumber + ". " + diagnostic(ex);
    }

    private static String diagnostic(Throwable ex) {
        if (ex instanceof PdfRenderException pdfEx) {
            return "[" + pdfEx.code() + "] " + pdfEx.getMessage();
        }
        String message = ex == null ? "" : ex.getMessage();
        return message == null || message.isBlank() ? "Error de renderizado." : message;
    }

    private final class PdfPageSlot {
        private final PdfVisualPage page;
        private final VBox root = new VBox(8);
        private final StackPane frame = new StackPane();
        private final ImageView imageView = new ImageView();
        private final Pane selectionOverlay = new Pane();
        private final Rectangle hoverTextHighlightRectangle = new Rectangle();
        private final Rectangle hoverTextUnderlineRectangle = new Rectangle();
        private final Rectangle fixedTextHighlightRectangle = new Rectangle();
        private final Rectangle fixedTextUnderlineRectangle = new Rectangle();
        private final Pane textHighlightOverlay = new Pane();
        private final Rectangle selectionRectangle = new Rectangle();
        private final Pane narrationFocusOverlay = new Pane();
        private double selectionStartX;
        private double selectionStartY;
        private boolean hasImage;

        private PdfPageSlot(PdfVisualPage page) {
            this.page = page;
            Label pageLabel = new Label("Pagina " + page.pageNumber());
            pageLabel.getStyleClass().add("pdf-visual-page-number");
            root.getStyleClass().add("pdf-visual-page-slot");
            root.setAlignment(Pos.TOP_CENTER);
            frame.getStyleClass().add("pdf-visual-page-frame");
            frame.setPickOnBounds(true);
            frame.setMinHeight(placeholderHeight(page));
            frame.setPrefHeight(placeholderHeight(page));
            imageView.getStyleClass().add("pdf-visual-page-image");
            imageView.setPreserveRatio(true);
            imageView.setSmooth(true);
            imageView.setPickOnBounds(true);
            selectionOverlay.setManaged(false);
            selectionOverlay.setPickOnBounds(true);
            selectionOverlay.setMouseTransparent(true);
            selectionOverlay.getStyleClass().add("pdf-visual-region-overlay");
            configureTextOverlay(hoverTextHighlightRectangle, "pdf-visual-text-hover");
            configureTextOverlay(hoverTextUnderlineRectangle, "pdf-visual-text-hover-underline");
            configureTextOverlay(fixedTextHighlightRectangle, "pdf-visual-text-selected");
            configureTextOverlay(fixedTextUnderlineRectangle, "pdf-visual-text-selected-underline");
            textHighlightOverlay.setManaged(false);
            textHighlightOverlay.setMouseTransparent(true);
            textHighlightOverlay.setVisible(false);
            textHighlightOverlay.setAccessibleRole(AccessibleRole.TEXT);
            selectionRectangle.getStyleClass().add("pdf-visual-region-selection");
            selectionRectangle.setManaged(false);
            selectionRectangle.setMouseTransparent(true);
            selectionRectangle.setVisible(false);
            narrationFocusOverlay.setManaged(false);
            narrationFocusOverlay.setMouseTransparent(true);
            narrationFocusOverlay.setVisible(false);
            narrationFocusOverlay.getStyleClass().add("pdf-narration-focus-overlay");
            narrationFocusOverlay.setAccessibleRole(AccessibleRole.TEXT);
            imageView.fitWidthProperty().bind(Bindings.createDoubleBinding(
                    PdfVisualDocumentView.this::visiblePageWidthForZoom,
                    scroll.viewportBoundsProperty(),
                    widthProperty(),
                    zoomScale));
            imageView.boundsInParentProperty().addListener((obs, oldValue, newValue) -> {
                updateSelectionOverlay();
                updateTextTargetOverlays();
            });
            installRegionSelectionHandlers();
            installTextTargetHandlers();
            root.getChildren().addAll(pageLabel, frame);
            showWaiting();
        }

        private PdfVisualPage page() {
            return page;
        }

        private Image image() {
            return imageView.getImage();
        }

        private Node root() {
            return root;
        }

        private void configureTextOverlay(Rectangle rectangle, String styleClass) {
            rectangle.getStyleClass().add(styleClass);
            rectangle.setFill(javafx.scene.paint.Color.TRANSPARENT);
            rectangle.setStroke(javafx.scene.paint.Color.TRANSPARENT);
            rectangle.setManaged(false);
            rectangle.setMouseTransparent(true);
            rectangle.setVisible(false);
        }

        private void showWaiting() {
            if (hasImage) {
                return;
            }
            Label label = new Label("Pagina lista para renderizar.");
            label.getStyleClass().add("pdf-visual-page-placeholder");
            frame.getChildren().setAll(label);
        }

        private void showLoading() {
            if (hasImage) {
                return;
            }
            ProgressIndicator indicator = StudioFeedbackControls.progressIndicator();
            indicator.setMaxSize(28, 28);
            Label label = new Label("Renderizando pagina PDF...");
            label.getStyleClass().add("pdf-visual-page-placeholder");
            VBox box = new VBox(8, indicator, label);
            box.setAlignment(Pos.CENTER);
            frame.getChildren().setAll(box);
        }

        private void showImage(Image image) {
            imageView.setImage(image);
            hasImage = true;
            frame.setPrefHeight(StackPane.USE_COMPUTED_SIZE);
            selectionRectangle.setVisible(false);
            frame.getChildren().setAll(
                    imageView,
                    hoverTextHighlightRectangle,
                    hoverTextUnderlineRectangle,
                    fixedTextHighlightRectangle,
                    fixedTextUnderlineRectangle,
                    textHighlightOverlay,
                    narrationFocusOverlay,
                    selectionOverlay,
                    selectionRectangle);
            updateSelectionOverlay();
            updateRegionSelectionState();
            updateTextTargetOverlays();
            updateNarrationFocus();
        }

        private void showError(String text) {
            hasImage = false;
            Label label = new Label(text);
            label.setWrapText(true);
            label.getStyleClass().add("pdf-visual-page-error");
            frame.getChildren().setAll(label);
        }

        private void unloadImage() {
            if (!hasImage) {
                return;
            }
            imageView.setImage(null);
            hasImage = false;
            hoverTextHighlightRectangle.setVisible(false);
            hoverTextUnderlineRectangle.setVisible(false);
            fixedTextHighlightRectangle.setVisible(false);
            fixedTextUnderlineRectangle.setVisible(false);
            textHighlightOverlay.setVisible(false);
            textHighlightOverlay.getChildren().clear();
            narrationFocusOverlay.setVisible(false);
            narrationFocusOverlay.getChildren().clear();
            selectionRectangle.setVisible(false);
            showWaiting();
        }

        private void reset() {
            imageView.setImage(null);
            hasImage = false;
            hoverTextHighlightRectangle.setVisible(false);
            hoverTextUnderlineRectangle.setVisible(false);
            fixedTextHighlightRectangle.setVisible(false);
            fixedTextUnderlineRectangle.setVisible(false);
            textHighlightOverlay.setVisible(false);
            textHighlightOverlay.getChildren().clear();
            narrationFocusOverlay.setVisible(false);
            narrationFocusOverlay.getChildren().clear();
            selectionRectangle.setVisible(false);
            showWaiting();
        }

        private void updateTextHighlight() {
            PdfVisualTextHighlight highlight = activeTextHighlight;
            textHighlightOverlay.getChildren().clear();
            if (highlight == null || !highlight.available() || !hasImage || imageView.getImage() == null
                    || highlight.pageNumber() != page.pageNumber()) {
                textHighlightOverlay.setVisible(false);
                return;
            }
            textHighlightOverlay.resizeRelocate(0, 0,
                    frame.getWidth(), frame.getHeight());
            textHighlightOverlay.setAccessibleText(highlight.text());
            PdfPageRegion visibleRegion = highlight.visibleRegion();
            Rectangle rectangle = new Rectangle();
            Rectangle underline = new Rectangle();
            configurePlaybackHighlightRectangle(rectangle, "pdf-visual-text-highlight");
            configurePlaybackHighlightRectangle(underline,
                    "pdf-visual-text-highlight-underline");
            updateTextOverlay(visibleRegion, rectangle, underline,
                    highlight.origin() == PdfTextLayerOrigin.OCR_LOCAL);
            textHighlightOverlay.getChildren().addAll(rectangle, underline);
            textHighlightOverlay.setVisible(true);
            PdfPlaybackVisualGuard.Snapshot expected = playbackVisualGuard.snapshot();
            if (!expected.expectedRegionId().isBlank()) {
                boolean matches = playbackVisualGuard.painted(
                        activePlaybackPaintSequence, expected.expectedRegionId());
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("PDF highlight painted sequence={} expectedVisualRegionId={} "
                                    + "paintedVisualRegionId={} identityMatches={} page={} timestampNanos={}",
                            activePlaybackPaintSequence, expected.expectedRegionId(),
                            expected.expectedRegionId(), matches, page.pageNumber(), System.nanoTime());
                }
            }
        }

        private void updateTextTargetOverlays() {
            updateHoverTextTarget();
            updateFixedTextTarget();
            updateTextHighlight();
            updateNarrationFocus();
        }

        private void updateNarrationFocus() {
            PdfNarrationFocusRef focus = activeNarrationFocus;
            narrationFocusOverlay.getChildren().clear();
            if (focus == null || focus.pageNumber() != page.pageNumber()
                    || !hasImage || imageView.getImage() == null) {
                narrationFocusOverlay.setVisible(false);
                return;
            }
            if (!shouldShowNarrationFocus(activeTextHighlight, page.pageNumber())) {
                narrationFocusOverlay.setVisible(false);
                return;
            }
            Bounds display = displayedImageBounds();
            if (display == null) {
                narrationFocusOverlay.setVisible(false);
                return;
            }
            narrationFocusOverlay.resizeRelocate(0, 0, frame.getWidth(), frame.getHeight());
            narrationFocusOverlay.setAccessibleText(focus.accessibleLabel());
            for (PdfNarrationFocusRef.FocusBox box : focus.boxes()) {
                PdfPageRegion region = new PdfPageRegion(
                        page.pageNumber(), box.xMin(), box.yMin(), box.xMax(), box.yMax(),
                        page.widthPoints(), page.heightPoints());
                PdfPageCoordinateTransform.ViewportBox viewportBox = viewportBox(region, display);
                Rectangle rectangle = new Rectangle();
                configureNarrationFocusRectangle(rectangle);
                rectangle.setX(viewportBox.xMin());
                rectangle.setY(viewportBox.yMin());
                rectangle.setWidth(Math.max(3.0, viewportBox.width()));
                rectangle.setHeight(Math.max(3.0, viewportBox.height()));
                Label label = new Label(focus.accessibleLabel());
                label.getStyleClass().add("pdf-narration-focus-label");
                label.setManaged(false);
                label.relocate(rectangle.getX(), Math.max(display.getMinY(), rectangle.getY() - 25.0));
                narrationFocusOverlay.getChildren().addAll(rectangle, label);
            }
            narrationFocusOverlay.setVisible(true);
            narrationFocusOverlay.toFront();
        }

        private void updateHoverTextTarget() {
            updateTextTarget(shouldShowHover(activeTextHighlight, hoverTextTarget)
                            ? hoverTextTarget : null,
                    hoverTextHighlightRectangle, hoverTextUnderlineRectangle);
        }

        private void updateFixedTextTarget() {
            updateTextTarget(fixedTextTarget, fixedTextHighlightRectangle, fixedTextUnderlineRectangle);
        }

        private void updateTextTarget(PdfVisualTextTarget target, Rectangle rectangle, Rectangle underline) {
            if (target == null || !target.available() || !hasImage || imageView.getImage() == null
                    || target.pageNumber() != page.pageNumber()) {
                rectangle.setVisible(false);
                underline.setVisible(false);
                return;
            }
            PdfPageRegion visibleRegion = textOverlayRegion(target);
            updateTextOverlay(visibleRegion, rectangle, underline,
                    target.origin() == PdfTextLayerOrigin.OCR_LOCAL);
        }

        private void installRegionSelectionHandlers() {
            selectionOverlay.addEventFilter(MouseEvent.MOUSE_PRESSED, this::beginRegionSelection);
            selectionOverlay.addEventFilter(MouseEvent.MOUSE_DRAGGED, this::dragRegionSelection);
            selectionOverlay.addEventFilter(MouseEvent.MOUSE_RELEASED, this::finishRegionSelection);
            frame.addEventFilter(MouseEvent.MOUSE_PRESSED, this::beginRegionSelection);
            frame.addEventFilter(MouseEvent.MOUSE_DRAGGED, this::dragRegionSelection);
            frame.addEventFilter(MouseEvent.MOUSE_RELEASED, this::finishRegionSelection);
        }

        private void installTextTargetHandlers() {
            frame.addEventFilter(MouseEvent.MOUSE_MOVED, this::hoverTextTarget);
            frame.addEventFilter(MouseEvent.MOUSE_EXITED, event -> {
                if (!regionSelectionActive) {
                    clearHoverTextTarget();
                    frame.setCursor(Cursor.DEFAULT);
                }
            });
            frame.addEventFilter(MouseEvent.MOUSE_CLICKED, this::clickTextTarget);
            frame.addEventFilter(
                    MouseEvent.MOUSE_CLICKED, this::showTextTargetContextMenu);
        }

        private void hoverTextTarget(MouseEvent event) {
            if (!canHitTestText()) {
                clearHoverTextTarget();
                return;
            }
            PdfVisualTextTarget target = targetAt(event);
            showHoverTextTarget(target);
            frame.setCursor(target == null ? Cursor.DEFAULT : Cursor.HAND);
        }

        private void clickTextTarget(MouseEvent event) {
            if (event.getButton() != MouseButton.PRIMARY || !canHitTestText()) {
                return;
            }
            dismissTextTargetContextMenu();
            PdfVisualTextTarget target = targetAt(event);
            requestFocus();
            if (target == null) {
                clearPinnedTextTarget();
                if (emptyTextTargetSelectionHandler != null) {
                    emptyTextTargetSelectionHandler.run();
                }
                event.consume();
                return;
            }
            showPinnedTextTarget(target);
            if (textTargetSelectionHandler != null) {
                textTargetSelectionHandler.accept(target);
            }
            event.consume();
        }

        private void showTextTargetContextMenu(MouseEvent event) {
            if (event.getButton() != MouseButton.SECONDARY
                    || !canHitTestText()) {
                return;
            }
            dismissTextTargetContextMenu();
            PdfVisualTextTarget target = targetAt(event);
            if (target == null) {
                clearPinnedTextTarget();
                if (emptyTextTargetSelectionHandler != null) {
                    emptyTextTargetSelectionHandler.run();
                }
                event.consume();
                return;
            }
            showPinnedTextTarget(target);
            if (textTargetSelectionHandler != null) {
                textTargetSelectionHandler.accept(target);
            }
            boolean secondary = target.kind()
                    == com.marcosmoreiradev.docupodcaststudio.application.document
                    .PdfVisualTextTargetKind.SEMANTIC_COMPONENT;
            activeTextTargetContextMenu = DocumentSelectionContextMenuFactory.create(
                    secondary, false,
                    narrateFromTargetRequestHandler == null ? null
                            : () -> narrateFromTargetRequestHandler.accept(target),
                    () -> copyTargetText(target),
                    secondary && contentDescriptionRequestHandler != null
                            ? () -> contentDescriptionRequestHandler.accept(target) : null,
                    secondary && manualDescriptionRequestHandler != null
                            ? () -> manualDescriptionRequestHandler.accept(target) : null);
            activeTextTargetContextMenu.setOnHidden(hidden -> {
                if (activeTextTargetContextMenu != null
                        && !activeTextTargetContextMenu.isShowing()) {
                    activeTextTargetContextMenu = null;
                }
            });
            activeTextTargetContextMenu.show(
                    frame, event.getScreenX(), event.getScreenY());
            event.consume();
        }

        private void copyTargetText(PdfVisualTextTarget target) {
            ClipboardContent content = new ClipboardContent();
            content.putString(target == null ? "" : target.text());
            Clipboard.getSystemClipboard().setContent(content);
        }

        private boolean canHitTestText() {
            return !regionSelectionActive && hasImage && imageView.getImage() != null && readingProjection != null;
        }

        private PdfVisualTextTarget targetAt(MouseEvent event) {
            Point2D imagePoint = imagePointFromEvent(event, false);
            Point2D pagePoint = pagePointFromImagePoint(imagePoint);
            if (pagePoint == null) {
                return null;
            }
            return readingProjection.targetAt(page.pageNumber(), pagePoint.getX(), pagePoint.getY()).orElse(null);
        }

        private void requestTextPreparation(MouseEvent event) {
            if (textPreparationRequestHandler == null) {
                return;
            }
            Point2D imagePoint = imagePointFromEvent(event, false);
            Point2D pagePoint = pagePointFromImagePoint(imagePoint);
            if (pagePoint == null) {
                return;
            }
            pendingTextTargetRequest = new PendingTextTargetRequest(page.pageNumber(), pagePoint.getX(), pagePoint.getY());
            textPreparationRequestHandler.accept(page.pageNumber());
            event.consume();
        }

        private void beginRegionSelection(MouseEvent event) {
            if (!canSelectRegion()) {
                return;
            }
            Point2D point = imagePointFromEvent(event, false);
            if (point == null) {
                return;
            }
            selectionStartX = point.getX();
            selectionStartY = point.getY();
            updateSelectionRectangle(selectionStartX, selectionStartY, selectionStartX, selectionStartY);
            event.consume();
        }

        private void dragRegionSelection(MouseEvent event) {
            if (!canSelectRegion() || !selectionRectangle.isVisible()) {
                return;
            }
            Point2D point = imagePointFromEvent(event, true);
            if (point == null) {
                return;
            }
            updateSelectionRectangle(selectionStartX, selectionStartY, point.getX(), point.getY());
            event.consume();
        }

        private void finishRegionSelection(MouseEvent event) {
            if (!canSelectRegion() || !selectionRectangle.isVisible()) {
                return;
            }
            Point2D point = imagePointFromEvent(event, true);
            if (point == null) {
                selectionRectangle.setVisible(false);
                return;
            }
            double endX = point.getX();
            double endY = point.getY();
            selectionRectangle.setVisible(false);
            if (Math.abs(endX - selectionStartX) < MIN_REGION_SELECTION_SIZE
                    || Math.abs(endY - selectionStartY) < MIN_REGION_SELECTION_SIZE) {
                event.consume();
                return;
            }
            Image image = imageView.getImage();
            if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
                event.consume();
                return;
            }
            PdfViewportSelection selection = new PdfViewportSelection(
                    page.pageNumber(),
                    selectionStartX,
                    selectionStartY,
                    endX,
                    endY,
                    image.getWidth(),
                    image.getHeight(),
                    page.widthPoints(),
                    page.heightPoints());
            regionSelectionHandler.accept(selection);
            event.consume();
        }

        private boolean canSelectRegion() {
            return regionSelectionActive && hasImage && imageView.getImage() != null && regionSelectionHandler != null;
        }

        private Point2D imagePointFromEvent(MouseEvent event, boolean clamp) {
            if (event.getSource() == selectionOverlay) {
                Point2D overlayPoint = imagePointFromOverlay(event.getX(), event.getY(), clamp);
                if (overlayPoint != null) {
                    return overlayPoint;
                }
            }
            Point2D framePoint = frame.sceneToLocal(event.getSceneX(), event.getSceneY());
            return imagePointFromFrame(framePoint.getX(), framePoint.getY(), clamp);
        }

        private Point2D imagePointFromScene(double sceneX, double sceneY, boolean clamp) {
            Point2D framePoint = frame.sceneToLocal(sceneX, sceneY);
            return imagePointFromFrame(framePoint.getX(), framePoint.getY(), clamp);
        }

        private Point2D imagePointFromFrame(double frameX, double frameY, boolean clamp) {
            Bounds displayBounds = displayedImageBounds();
            Image image = imageView.getImage();
            if (displayBounds == null || image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
                return null;
            }
            if (!clamp && !displayBounds.contains(frameX, frameY)) {
                return null;
            }
            double framePointX = Math.max(displayBounds.getMinX(), Math.min(displayBounds.getMaxX(), frameX));
            double framePointY = Math.max(displayBounds.getMinY(), Math.min(displayBounds.getMaxY(), frameY));
            double x = (framePointX - displayBounds.getMinX()) * image.getWidth() / Math.max(1.0, displayBounds.getWidth());
            double y = (framePointY - displayBounds.getMinY()) * image.getHeight() / Math.max(1.0, displayBounds.getHeight());
            return new Point2D(
                    Math.max(0.0, Math.min(image.getWidth(), x)),
                    Math.max(0.0, Math.min(image.getHeight(), y)));
        }

        private Bounds displayedImageBounds() {
            Bounds bounds = imageView.localToParent(imageView.getLayoutBounds());
            if (bounds == null || bounds.getWidth() <= 0 || bounds.getHeight() <= 0) {
                bounds = imageView.getBoundsInParent();
            }
            if (bounds == null || bounds.getWidth() <= 0 || bounds.getHeight() <= 0) {
                return null;
            }
            return bounds;
        }

        private Bounds displayedImageSceneBounds() {
            Bounds parentBounds = displayedImageBounds();
            if (parentBounds != null && parentBounds.getWidth() > 0 && parentBounds.getHeight() > 0) {
                return frame.localToScene(parentBounds);
            }
            Bounds bounds = imageView.localToScene(imageView.getBoundsInLocal());
            if (bounds == null || bounds.getWidth() <= 0 || bounds.getHeight() <= 0) {
                return null;
            }
            return bounds;
        }

        private Point2D imagePointFromOverlay(double overlayX, double overlayY, boolean clamp) {
            Image image = imageView.getImage();
            double width = selectionOverlay.getWidth();
            double height = selectionOverlay.getHeight();
            if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0 || width <= 0 || height <= 0) {
                return null;
            }
            if (!clamp && (overlayX < 0 || overlayY < 0 || overlayX > width || overlayY > height)) {
                return null;
            }
            double clampedX = Math.max(0.0, Math.min(width, overlayX));
            double clampedY = Math.max(0.0, Math.min(height, overlayY));
            return new Point2D(
                    clampedX * image.getWidth() / width,
                    clampedY * image.getHeight() / height);
        }

        private void updateSelectionRectangle(double x1, double y1, double x2, double y2) {
            Point2D start = imagePixelToFramePoint(x1, y1);
            Point2D end = imagePixelToFramePoint(x2, y2);
            if (start == null || end == null) {
                selectionRectangle.setVisible(false);
                return;
            }
            selectionRectangle.setX(Math.min(start.getX(), end.getX()));
            selectionRectangle.setY(Math.min(start.getY(), end.getY()));
            selectionRectangle.setWidth(Math.abs(end.getX() - start.getX()));
            selectionRectangle.setHeight(Math.abs(end.getY() - start.getY()));
            selectionRectangle.setVisible(true);
        }

        private Point2D imagePixelToFramePoint(double imageX, double imageY) {
            Bounds displayBounds = displayedImageBounds();
            Image image = imageView.getImage();
            if (displayBounds == null || image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
                return null;
            }
            double x = displayBounds.getMinX() + displayBounds.getWidth() * imageX / image.getWidth();
            double y = displayBounds.getMinY() + displayBounds.getHeight() * imageY / image.getHeight();
            return new Point2D(x, y);
        }

        private Point2D pagePointFromImagePoint(Point2D imagePoint) {
            Image image = imageView.getImage();
            if (imagePoint == null || image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
                return null;
            }
            PdfPageCoordinateTransform.PagePoint point = coordinateTransform(image)
                    .rasterToPage(imagePoint.getX(), imagePoint.getY());
            return new Point2D(point.xPoints(), point.yPoints());
        }

        private Point2D imagePixelToScenePoint(double imageX, double imageY) {
            Point2D framePoint = imagePixelToFramePoint(imageX, imageY);
            return framePoint == null ? null : frame.localToScene(framePoint);
        }

        private void updateTextOverlay(com.marcosmoreiradev.docupodcaststudio.application.document.PdfPageRegion region,
                                       Rectangle rectangle,
                                       Rectangle underline,
                                       boolean playbackPadding) {
            Bounds displayBounds = displayedImageBounds();
            if (displayBounds == null) {
                rectangle.setVisible(false);
                underline.setVisible(false);
                return;
            }
            PdfPageCoordinateTransform.ViewportBox viewportBox = viewportBox(region, displayBounds);
            double horizontal = playbackPadding ? textPaddingHorizontalPixels() : 0.0;
            double vertical = playbackPadding ? textPaddingVerticalPixels() : 0.0;
            VisualBox visible = paddedAndClipped(new VisualBox(
                            viewportBox.xMin(), viewportBox.yMin(),
                            viewportBox.xMax(), viewportBox.yMax()),
                    new VisualBox(displayBounds.getMinX(), displayBounds.getMinY(),
                            displayBounds.getMaxX(), displayBounds.getMaxY()),
                    horizontal, vertical);
            double x = visible.xMin();
            double y = visible.yMin();
            double width = Math.max(2.0, visible.width());
            double height = Math.max(2.0, visible.height());
            rectangle.setX(x);
            rectangle.setY(y);
            rectangle.setWidth(width);
            rectangle.setHeight(height);
            rectangle.setVisible(true);
            underline.setX(x);
            underline.setY(y + Math.max(1.0, height - 2.0));
            underline.setWidth(width);
            underline.setHeight(2.0);
            underline.setVisible(true);
        }

        private Point2D regionTopScenePoint(PdfPageRegion region) {
            Bounds displayBounds = displayedImageBounds();
            if (displayBounds == null || region == null) {
                return null;
            }
            PdfPageCoordinateTransform.ViewportBox viewportBox = viewportBox(region, displayBounds);
            return frame.localToScene(viewportBox.xMin(), viewportBox.yMin());
        }

        private PdfPageCoordinateTransform coordinateTransform(Image image) {
            return new PdfPageCoordinateTransform(
                    page.widthPoints(), page.heightPoints(), page.rotationDegrees(),
                    image.getWidth(), image.getHeight());
        }

        private PdfPageCoordinateTransform.ViewportBox viewportBox(PdfPageRegion region, Bounds displayBounds) {
            Image image = imageView.getImage();
            if (image == null) {
                throw new IllegalStateException("PDF page image is not available");
            }
            return coordinateTransform(image).toViewport(
                    region,
                    displayBounds.getMinX(), displayBounds.getMinY(),
                    displayBounds.getWidth(), displayBounds.getHeight());
        }

        private void updateRegionSelectionState() {
            Cursor cursor = regionSelectionActive && hasImage ? Cursor.CROSSHAIR : Cursor.DEFAULT;
            imageView.setCursor(cursor);
            frame.setCursor(cursor);
            selectionOverlay.setCursor(cursor);
            selectionOverlay.setVisible(regionSelectionActive && hasImage);
            selectionOverlay.setMouseTransparent(!regionSelectionActive || !hasImage);
            if (!regionSelectionActive) {
                selectionRectangle.setVisible(false);
            }
        }

        private void updateSelectionOverlay() {
            Bounds bounds = displayedImageBounds();
            if (bounds == null) {
                selectionOverlay.resizeRelocate(0, 0, 0, 0);
                return;
            }
            selectionOverlay.resizeRelocate(bounds.getMinX(), bounds.getMinY(), bounds.getWidth(), bounds.getHeight());
            selectionOverlay.toFront();
            selectionRectangle.toFront();
        }
    }

    private void dismissTextTargetContextMenu() {
        ContextMenu menu = activeTextTargetContextMenu;
        activeTextTargetContextMenu = null;
        if (menu != null) menu.hide();
    }

    private static double placeholderHeight(PdfVisualPage page) {
        if (page.widthPoints() <= 0 || page.heightPoints() <= 0) {
            return 720.0;
        }
        double ratio = page.heightPoints() / page.widthPoints();
        return Math.max(360.0, Math.min(980.0, 620.0 * ratio));
    }

    private double viewportWidth() {
        Bounds bounds = scroll.getViewportBounds();
        double width = bounds == null || bounds.getWidth() <= 0 ? getWidth() : bounds.getWidth();
        return width <= 0 ? 900.0 : width;
    }

    static void configureNarrationFocusRectangle(Rectangle rectangle) {
        rectangle.getStyleClass().add("pdf-narration-focus-rectangle");
        // Programmatic fallback is intentional: a newly attached overlay must
        // never flash with Rectangle's opaque black default before CSS applies.
        rectangle.setFill(javafx.scene.paint.Color.rgb(250, 204, 21, 0.035));
        rectangle.setStroke(javafx.scene.paint.Color.web("#a16207"));
        rectangle.setStrokeWidth(4.0);
        rectangle.getStrokeDashArray().setAll(10.0, 6.0);
        rectangle.setManaged(false);
        rectangle.setMouseTransparent(true);
    }

    static void configurePlaybackHighlightRectangle(Rectangle rectangle, String styleClass) {
        rectangle.getStyleClass().add(styleClass);
        rectangle.setFill("pdf-visual-text-highlight-underline".equals(styleClass)
                ? javafx.scene.paint.Color.rgb(14, 165, 233, 0.72)
                : javafx.scene.paint.Color.rgb(125, 211, 252, 0.22));
        rectangle.setStroke(javafx.scene.paint.Color.TRANSPARENT);
        rectangle.setOpacity(1.0);
        rectangle.setManaged(false);
        rectangle.setMouseTransparent(true);
        rectangle.setVisible(false);
    }

    static double textPaddingHorizontalPixels() {
        return configuredPadding(TEXT_PADDING_HORIZONTAL_PROPERTY,
                DEFAULT_TEXT_PADDING_HORIZONTAL_PX);
    }

    static double textPaddingVerticalPixels() {
        return configuredPadding(TEXT_PADDING_VERTICAL_PROPERTY,
                DEFAULT_TEXT_PADDING_VERTICAL_PX);
    }

    private static double configuredPadding(String property, double fallback) {
        String configured = System.getProperty(property, "");
        if (configured.isBlank()) return fallback;
        try {
            double value = Double.parseDouble(configured);
            return Double.isFinite(value) ? Math.max(0.0, Math.min(40.0, value))
                    : fallback;
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    static VisualBox paddedAndClipped(VisualBox tight, VisualBox page,
                                      double horizontal, double vertical) {
        if (tight == null || page == null) throw new IllegalArgumentException("bounds required");
        double h = Math.max(0.0, horizontal);
        double v = Math.max(0.0, vertical);
        return new VisualBox(
                Math.max(page.xMin(), tight.xMin() - h),
                Math.max(page.yMin(), tight.yMin() - v),
                Math.min(page.xMax(), tight.xMax() + h),
                Math.min(page.yMax(), tight.yMax() + v));
    }

    record VisualBox(double xMin, double yMin, double xMax, double yMax) {
        VisualBox {
            if (xMax < xMin || yMax < yMin) throw new IllegalArgumentException("invalid visual bounds");
        }
        double width() { return xMax - xMin; }
        double height() { return yMax - yMin; }
    }

    static boolean shouldShowNarrationFocus(PdfVisualTextHighlight active, int pageNumber) {
        return active == null || !active.available() || active.pageNumber() != pageNumber;
    }

    /** Shared geometry authority for hover and fixed selection; playback uses the same highlight. */
    static PdfPageRegion textOverlayRegion(PdfVisualTextTarget target) {
        return target == null ? null : target.highlight().visibleRegion();
    }

    static boolean shouldShowHover(PdfVisualTextHighlight active, PdfVisualTextTarget hover) {
        if (hover == null || !hover.available()) return false;
        if (active == null || !active.available() || active.pageNumber() != hover.pageNumber()) {
            return true;
        }
        PdfPageRegion playback = active.visibleRegion();
        return playback == null || hover.highlightRegions().stream().noneMatch(region ->
                intersects(playback, region));
    }

    private static boolean intersects(PdfPageRegion a, PdfPageRegion b) {
        return a.xMinPoints() < b.xMaxPoints() && a.xMaxPoints() > b.xMinPoints()
                && a.yMinPoints() < b.yMaxPoints() && a.yMaxPoints() > b.yMinPoints();
    }

    private record PendingTextTargetRequest(int pageNumber, double xPoints, double yPoints) {
    }

    private record PageKey(Path sourcePath, int pageNumber, int dpi) {
    }
}

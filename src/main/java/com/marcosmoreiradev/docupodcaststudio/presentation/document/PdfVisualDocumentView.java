package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import com.marcosmoreiradev.docupodcaststudio.application.document.BuildPdfVisualDocumentUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfPageRenderRequest;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfPageRenderResult;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfPageRegion;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfRenderException;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfVisualReadingProjection;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfVisualTextHighlight;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfVisualTextTarget;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfViewportSelection;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfVisualDocument;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfVisualPage;
import com.marcosmoreiradev.docupodcaststudio.application.document.RenderPdfVisualPageUseCase;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
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
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
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
import java.util.function.IntSupplier;

/** Visual PDF reader surface used only when the source document is a PDF. */
public final class PdfVisualDocumentView extends BorderPane {
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
    private final ScrollPane scroll = new ScrollPane();
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

    private ReadableDocument currentDocument;
    private PdfVisualDocument visualDocument;
    private boolean regionSelectionActive;
    private PdfVisualTextHighlight activeTextHighlight;
    private PdfVisualReadingProjection readingProjection = new PdfVisualReadingProjection(List.of(), Map.of(), List.of());
    private PdfVisualTextTarget hoverTextTarget;
    private PdfVisualTextTarget fixedTextTarget;
    private PendingTextTargetRequest pendingTextTargetRequest;
    private Consumer<PdfVisualTextTarget> textTargetSelectionHandler;
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

    public void setTextPreparationRequestHandler(Consumer<Integer> handler) {
        textPreparationRequestHandler = handler;
    }

    public void showDocument(ReadableDocument document) {
        currentDocument = document;
        visualDocument = null;
        activeTextHighlight = null;
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
        if (document == null) {
            pages.getChildren().add(message("Sin PDF activo."));
            return;
        }
        try {
            visualDocument = buildPdfVisualDocument.build(document.sourcePath(), dpiForZoom());
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
        currentDocument = null;
        visualDocument = null;
        regionSelectionActive = false;
        activeRegionSelectionSlot = null;
        globalRegionSelectionRectangle.setVisible(false);
        updateGlobalRegionSelectionState();
        activeTextHighlight = null;
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
        activeTextHighlight = highlight == null || !highlight.available() ? null : highlight;
        slots.values().forEach(PdfPageSlot::updateTextHighlight);
        if (activeTextHighlight != null) {
            renderPage(activeTextHighlight.pageNumber());
        }
    }

    public void clearTextHighlight() {
        activeTextHighlight = null;
        slots.values().forEach(PdfPageSlot::updateTextHighlight);
    }

    public void showPinnedTextTarget(PdfVisualTextTarget target) {
        fixedTextTarget = target == null || !target.available() ? null : target;
        slots.values().forEach(PdfPageSlot::updateFixedTextTarget);
        if (fixedTextTarget != null) {
            renderPage(fixedTextTarget.pageNumber());
        }
    }

    public void clearPinnedTextTarget() {
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
        ReadableDocument document = currentDocument;
        PdfVisualDocument pdf = visualDocument;
        if (slot == null || document == null || pdf == null) {
            return;
        }
        int dpi = dpiForVisibleWidth(slot.page());
        PageKey key = new PageKey(document.sourcePath(), pageNumber, dpi);
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
        ReadableDocument document = currentDocument;
        PdfVisualDocument pdf = visualDocument;
        if (slot == null || document == null || pdf == null) {
            return;
        }
        int dpi = dpiForVisibleWidth(slot.page());
        PageKey key = new PageKey(document.sourcePath(), pageNumber, dpi);
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
        Path sourcePath = document.sourcePath();
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
        System.out.println("[PdfVisualRender] " + event
                + " active=" + activeRenderTasks
                + " queued=" + renderQueue.size()
                + " rendering=" + renderingPages.size()
                + " cache=" + pageCache.size());
    }

    private boolean sameDocument(Path sourcePath) {
        return currentDocument != null && currentDocument.sourcePath().equals(sourcePath);
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
        private final Rectangle textHighlightRectangle = new Rectangle();
        private final Rectangle textHighlightUnderlineRectangle = new Rectangle();
        private final Rectangle selectionRectangle = new Rectangle();
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
            configureTextOverlay(textHighlightRectangle, "pdf-visual-text-highlight");
            configureTextOverlay(textHighlightUnderlineRectangle, "pdf-visual-text-highlight-underline");
            selectionRectangle.getStyleClass().add("pdf-visual-region-selection");
            selectionRectangle.setManaged(false);
            selectionRectangle.setMouseTransparent(true);
            selectionRectangle.setVisible(false);
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
            ProgressIndicator indicator = new ProgressIndicator();
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
                    textHighlightRectangle,
                    textHighlightUnderlineRectangle,
                    selectionOverlay,
                    selectionRectangle);
            updateSelectionOverlay();
            updateRegionSelectionState();
            updateTextTargetOverlays();
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
            textHighlightRectangle.setVisible(false);
            textHighlightUnderlineRectangle.setVisible(false);
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
            textHighlightRectangle.setVisible(false);
            textHighlightUnderlineRectangle.setVisible(false);
            selectionRectangle.setVisible(false);
            showWaiting();
        }

        private void updateTextHighlight() {
            PdfVisualTextHighlight highlight = activeTextHighlight;
            if (highlight == null || !highlight.available() || !hasImage || imageView.getImage() == null
                    || highlight.pageNumber() != page.pageNumber()) {
                textHighlightRectangle.setVisible(false);
                textHighlightUnderlineRectangle.setVisible(false);
                return;
            }
            updateTextOverlay(highlight.region(), textHighlightRectangle, textHighlightUnderlineRectangle);
        }

        private void updateTextTargetOverlays() {
            updateHoverTextTarget();
            updateFixedTextTarget();
            updateTextHighlight();
        }

        private void updateHoverTextTarget() {
            updateTextTarget(hoverTextTarget, hoverTextHighlightRectangle, hoverTextUnderlineRectangle);
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
            updateTextOverlay(target.region(), rectangle, underline);
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
            PdfVisualTextTarget target = targetAt(event);
            if (target == null) {
                requestTextPreparation(event);
                return;
            }
            showPinnedTextTarget(target);
            if (textTargetSelectionHandler != null) {
                textTargetSelectionHandler.accept(target);
            }
            event.consume();
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
            double x = imagePoint.getX() * page.widthPoints() / image.getWidth();
            double y = imagePoint.getY() * page.heightPoints() / image.getHeight();
            return new Point2D(x, y);
        }

        private Point2D imagePixelToScenePoint(double imageX, double imageY) {
            Point2D framePoint = imagePixelToFramePoint(imageX, imageY);
            return framePoint == null ? null : frame.localToScene(framePoint);
        }

        private void updateTextOverlay(com.marcosmoreiradev.docupodcaststudio.application.document.PdfPageRegion region,
                                       Rectangle rectangle,
                                       Rectangle underline) {
            Bounds displayBounds = displayedImageBounds();
            if (displayBounds == null) {
                rectangle.setVisible(false);
                underline.setVisible(false);
                return;
            }
            double x1 = displayBounds.getMinX() + displayBounds.getWidth() * region.xMinPoints() / Math.max(1.0, region.pageWidthPoints());
            double y1 = displayBounds.getMinY() + displayBounds.getHeight() * region.yMinPoints() / Math.max(1.0, region.pageHeightPoints());
            double x2 = displayBounds.getMinX() + displayBounds.getWidth() * region.xMaxPoints() / Math.max(1.0, region.pageWidthPoints());
            double y2 = displayBounds.getMinY() + displayBounds.getHeight() * region.yMaxPoints() / Math.max(1.0, region.pageHeightPoints());
            double x = Math.min(x1, x2);
            double y = Math.min(y1, y2);
            double width = Math.max(2.0, Math.abs(x2 - x1));
            double height = Math.max(2.0, Math.abs(y2 - y1));
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
            double pageWidth = Math.max(1.0, page.widthPoints());
            double pageHeight = Math.max(1.0, page.heightPoints());
            double x = displayBounds.getMinX()
                    + displayBounds.getWidth() * Math.max(0.0, Math.min(pageWidth, region.xMinPoints())) / pageWidth;
            double y = displayBounds.getMinY()
                    + displayBounds.getHeight() * Math.max(0.0, Math.min(pageHeight, region.yMinPoints())) / pageHeight;
            return frame.localToScene(x, y);
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

    private record PendingTextTargetRequest(int pageNumber, double xPoints, double yPoints) {
    }

    private record PageKey(Path sourcePath, int pageNumber, int dpi) {
    }
}

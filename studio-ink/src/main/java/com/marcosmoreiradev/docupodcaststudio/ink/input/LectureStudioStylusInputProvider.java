package com.marcosmoreiradev.docupodcaststudio.ink.input;

import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.Window;
import org.lecturestudio.stylus.StylusAxesData;
import org.lecturestudio.stylus.StylusButton;
import org.lecturestudio.stylus.StylusCursor;
import org.lecturestudio.stylus.StylusEvent;
import org.lecturestudio.stylus.StylusListener;
import org.lecturestudio.stylus.StylusManager;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Optional adapter boundary for LectureStudio stylus support.
 *
 * <p>No study/theatre UI class should import the native library directly. The
 * adapter reports native capabilities only after a real stylus event arrives.</p>
 */
public final class LectureStudioStylusInputProvider implements InkInputProvider {
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(LectureStudioStylusInputProvider.class);
    private static final String DISABLE_PROPERTY = "docupodcast.ink.disableNativeStylus";
    private static final String NATIVE_LIBRARY_RESOURCE = "stylus.dll";
    private static final long FALLBACK_SUPPRESSION_NANOS = 150_000_000L;
    private static final int MAX_NATIVE_EVENTS_PER_PULSE = 192;
    private static final long MAX_NATIVE_DRAIN_NANOS = 4_000_000L;
    private static final double MIN_STYLUS_CONTACT_PRESSURE = 0.01;
    private static final boolean INK_INPUT_DIAGNOSTICS =
            Boolean.getBoolean("docupodcast.ink.inputDiagnostics");

    private final InkInputProvider fallback;
    private final boolean strict;
    private final AtomicBoolean nativeEventsSeen = new AtomicBoolean(false);
    private final AtomicBoolean stylusDrainScheduled = new AtomicBoolean(false);
    private final AtomicLong lastNativeEventNanos = new AtomicLong(0L);
    private final Queue<PendingStylusEvent> pendingStylusEvents = new ConcurrentLinkedQueue<>();
    private final NativeInkCoordinateSpaceResolver coordinateResolver = new NativeInkCoordinateSpaceResolver();

    private Node target;
    private StylusListener stylusListener;
    private StylusManager stylusManager;
    private InkInputListener listener;
    private EventHandler<MouseEvent> coordinateAnchorHandler;
    private EventHandler<MouseEvent> scenePrimaryButtonHandler;
    private Scene observedScene;
    private Window observedWindow;
    private Window attachedWindow;
    private Long attachedWindowHandle;
    private InvalidationListener sceneListener;
    private InvalidationListener windowListener;
    private ChangeListener<Boolean> showingListener;
    private ChangeListener<Boolean> focusedListener;
    private boolean strokeActive;
    private boolean nativePrimaryButtonDown;
    private boolean javafxPrimaryButtonDown;
    private boolean javafxPrimaryPressStartedInsideTarget;
    private boolean nativeAttached;
    private boolean nativeAttachFailed;
    private String fallbackReason;
    private Point2D lastAcceptedTargetLocalPoint;
    private String lastCoordinateSpaceLabel = "sin-coords";
    private InkInputCursor lastInputCursor = InkInputCursor.UNKNOWN;
    private double lastInputRawPressure = Double.NaN;

    private LectureStudioStylusInputProvider(InkInputProvider fallback, boolean strict) {
        this.fallback = fallback == null ? NoopInkInputProvider.INSTANCE : fallback;
        this.strict = strict;
        this.fallbackReason = waitingReason(strict);
    }

    public static Optional<InkInputProvider> tryCreate() {
        return tryCreate(NoopInkInputProvider.INSTANCE);
    }

    public static Optional<InkInputProvider> tryCreate(InkInputProvider fallback) {
        if (Boolean.getBoolean(DISABLE_PROPERTY)) {
            return Optional.empty();
        }
        if (!System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")) {
            return Optional.empty();
        }
        stylusModuleNativeResourceFailure().ifPresent(LectureStudioStylusInputProvider::reportInputDiagnostic);
        return Optional.of(new LectureStudioStylusInputProvider(fallback, false));
    }

    public static InkInputProvider createStrictOrUnavailable() {
        if (Boolean.getBoolean(DISABLE_PROPERTY)) {
            return unavailable("LectureStudio stylus deshabilitado por " + DISABLE_PROPERTY + ".");
        }
        if (!System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")) {
            return unavailable("LectureStudio stylus solo esta habilitado para Windows x86_64 en esta version.");
        }
        stylusModuleNativeResourceFailure().ifPresent(LectureStudioStylusInputProvider::reportInputDiagnostic);
        reportInputDiagnostic("LectureStudio provider estricto creado; esperando ventana para enganchar stylus nativo.");
        return new LectureStudioStylusInputProvider(NoopInkInputProvider.INSTANCE, true);
    }

    public static Optional<String> probeNativeClasses() {
        Optional<String> nativeResourceFailure = stylusModuleNativeResourceFailure();
        if (nativeResourceFailure.isPresent()) {
            return nativeResourceFailure;
        }
        try {
            StylusManager.getInstance().getDevices();
            return Optional.empty();
        } catch (RuntimeException | LinkageError ex) {
            return Optional.of(ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }
    }

    @Override
    public InkInputCapabilities capabilities() {
        if (nativeEventsSeen.get()) {
            return InkInputCapabilities.lectureStudioStylus();
        }
        if (strict) {
            return nativeAttachFailed
                    ? InkInputCapabilities.lectureStudioUnavailable(fallbackReason)
                    : InkInputCapabilities.lectureStudioWaiting(fallbackReason);
        }
        return InkInputCapabilities.javafxMouse(fallbackReason);
    }

    @Override
    public void attach(Node target, InkInputListener listener) {
        detach();
        if (target == null || listener == null) {
            return;
        }
        this.target = target;
        this.listener = listener;
        coordinateResolver.reset();
        installCoordinateAnchor(target);
        if (!strict) {
            fallback.attach(target, new SuppressingFallbackListener(listener));
        }
        stylusListener = new NativeStylusListener();
        installNativeAttachHooks();
    }

    @Override
    public void detach() {
        finishActiveStrokeFromState("detach");
        clearPointerState("detach");
        clearNativeAttachHooks();
        if (nativeAttached && stylusManager != null && stylusListener != null && attachedWindowHandle != null) {
            try {
                stylusManager.detachStylusListener(stylusListener, Long.valueOf(attachedWindowHandle));
            } catch (Throwable ignored) {
                // Best effort during dialog close.
            }
        }
        if (!strict) {
            fallback.detach();
        }
        pendingStylusEvents.clear();
        stylusDrainScheduled.set(false);
        removeCoordinateAnchor();
        coordinateResolver.reset();
        target = null;
        stylusListener = null;
        stylusManager = null;
        listener = null;
        observedScene = null;
        observedWindow = null;
        attachedWindow = null;
        attachedWindowHandle = null;
        strokeActive = false;
        lastAcceptedTargetLocalPoint = null;
        lastCoordinateSpaceLabel = "sin-coords";
        lastInputCursor = InkInputCursor.UNKNOWN;
        lastInputRawPressure = Double.NaN;
        nativeAttached = false;
        nativeAttachFailed = false;
    }

    @Override
    public void resetCoordinateState() {
        finishActiveStrokeFromState("reset coordenadas");
        clearPointerState("reset coordenadas");
        coordinateResolver.reset();
        pendingStylusEvents.clear();
        stylusDrainScheduled.set(false);
        lastAcceptedTargetLocalPoint = null;
        lastCoordinateSpaceLabel = "sin-coords";
    }

    static InkInputSample sampleFromStylusEvent(StylusEvent event, boolean primaryButtonDown) {
        return sampleFromStylusEvent(event, primaryButtonDown, "LectureStudio");
    }

    static InkInputSample sampleFromStylusEvent(StylusEvent event, boolean primaryButtonDown, String inputSource) {
        StylusAxesData axes = event == null ? null : event.getAxesData();
        StylusCursor stylusCursor = event == null ? StylusCursor.NONE : event.getCursor();
        double rawPressure = axes == null ? 1.0 : axes.getPressure();
        double normalizedPressure = stylusCursor == StylusCursor.MOUSE
                ? 1.0
                : normalizePressure(rawPressure);
        boolean eraser = stylusCursor == StylusCursor.ERASER;
        InkInputCursor cursor = switch (stylusCursor) {
            case ERASER -> InkInputCursor.ERASER;
            case MOUSE -> InkInputCursor.MOUSE;
            case PEN -> InkInputCursor.PEN;
            default -> InkInputCursor.UNKNOWN;
        };
        return new InkInputSample(
                safeCoordinate(axes == null ? 0.0 : axes.getX()),
                safeCoordinate(axes == null ? 0.0 : axes.getY()),
                System.nanoTime(),
                normalizedPressure,
                cursor,
                primaryButtonDown,
                eraser,
                rawPressure,
                inputSource);
    }

    static boolean isPrimaryButton(StylusEvent event) {
        return event != null && event.getButton() == StylusButton.LEFT;
    }

    static boolean hasInkContact(StylusEvent event) {
        boolean implicitPrimary = event != null
                && event.getCursor() != StylusCursor.MOUSE
                && isPrimaryButton(event);
        return hasInkContact(event, implicitPrimary);
    }

    static boolean hasInkContact(StylusEvent event, boolean primaryButtonDown) {
        if (event == null) {
            return false;
        }
        if (event.getCursor() == StylusCursor.MOUSE) {
            return primaryButtonDown;
        }
        StylusAxesData axes = event.getAxesData();
        return normalizePressure(axes == null ? 0.0 : axes.getPressure()) > MIN_STYLUS_CONTACT_PRESSURE;
    }

    private void enqueueStylusEvent(StylusEventKind kind, StylusEvent event) {
        if (kind == null || event == null || listener == null) {
            return;
        }
        boolean firstNativeEvent = nativeEventsSeen.compareAndSet(false, true);
        if (firstNativeEvent) {
            reportInputDiagnostic("Primer evento nativo LectureStudio recibido: " + kind);
        }
        lastNativeEventNanos.set(System.nanoTime());
        pendingStylusEvents.add(new PendingStylusEvent(kind, event));
        if (!stylusDrainScheduled.compareAndSet(false, true)) {
            return;
        }
        if (Platform.isFxApplicationThread()) {
            drainStylusEvents();
        } else {
            Platform.runLater(this::drainStylusEvents);
        }
    }

    private void drainStylusEvents() {
        stylusDrainScheduled.set(false);
        InkInputListener activeListener = listener;
        if (activeListener == null) {
            pendingStylusEvents.clear();
            return;
        }
        List<InkInputSample> moveBatch = new ArrayList<>();
        PendingStylusEvent packet;
        int processed = 0;
        long drainStarted = System.nanoTime();
        while (processed < MAX_NATIVE_EVENTS_PER_PULSE
                && System.nanoTime() - drainStarted < MAX_NATIVE_DRAIN_NANOS
                && (packet = pendingStylusEvents.poll()) != null) {
            processed++;
            StylusEvent event = packet.event();
            boolean primaryButtonEvent = isPrimaryButton(event);
            InkInputCursor cursor = cursorFromStylusEvent(event);
            if (!translateToTarget(event, cursor)) {
                flushMoveBatch(activeListener, moveBatch);
                if (strokeActive && lastAcceptedTargetLocalPoint != null) {
                    finishActiveStroke(activeListener, null, "rechazo de coordenadas");
                }
                if (packet.kind() == StylusEventKind.BUTTON_UP && primaryButtonEvent) {
                    clearPointerState("button-up fuera de target");
                }
                continue;
            }
            if (!strokeActive && !eventInsideTarget(event)) {
                if (packet.kind() == StylusEventKind.BUTTON_UP && primaryButtonEvent) {
                    clearPointerState("button-up fuera de lienzo");
                } else if (packet.kind() == StylusEventKind.BUTTON_DOWN && primaryButtonEvent) {
                    clearPointerState("button-down fuera de lienzo");
                }
                continue;
            }
            if (packet.kind() == StylusEventKind.BUTTON_DOWN && primaryButtonEvent && eventInsideTarget(event)) {
                nativePrimaryButtonDown = true;
            }
            switch (packet.kind()) {
                case CURSOR_CHANGE -> {
                    flushMoveBatch(activeListener, moveBatch);
                    activeListener.onHover(sampleFromStylusEvent(event, strokeActive, inputSourceForCurrentPacket()));
                }
                case CURSOR_MOVE -> {
                    boolean contact = hasInkContactForPacket(event);
                    if (strokeActive && contact) {
                        moveBatch.add(sampleFromStylusEvent(event, true, inputSourceForCurrentPacket()));
                    } else if (strokeActive) {
                        flushMoveBatch(activeListener, moveBatch);
                        finishActiveStroke(activeListener, event, "sin contacto primario");
                        activeListener.onHover(sampleFromStylusEvent(event, false, inputSourceForCurrentPacket()));
                    } else if (contact) {
                        flushMoveBatch(activeListener, moveBatch);
                        strokeActive = activeListener.onStrokeStart(
                                sampleFromStylusEvent(event, true, inputSourceForCurrentPacket()));
                    } else {
                        flushMoveBatch(activeListener, moveBatch);
                        activeListener.onHover(sampleFromStylusEvent(event, false, inputSourceForCurrentPacket()));
                    }
                }
                case BUTTON_DOWN -> {
                    flushMoveBatch(activeListener, moveBatch);
                    if (!hasInkContactForPacket(event)) {
                        activeListener.onHover(sampleFromStylusEvent(event, false, inputSourceForCurrentPacket()));
                    } else {
                        strokeActive = activeListener.onStrokeStart(
                                sampleFromStylusEvent(event, true, inputSourceForCurrentPacket()));
                    }
                }
                case BUTTON_UP -> {
                    flushMoveBatch(activeListener, moveBatch);
                    if (!primaryButtonEvent || !strokeActive) {
                        activeListener.onHover(sampleFromStylusEvent(event, false, inputSourceForCurrentPacket()));
                    } else {
                        finishActiveStroke(activeListener, event, "button-up");
                    }
                    if (primaryButtonEvent) {
                        clearPointerState("button-up");
                    }
                }
            }
        }
        flushMoveBatch(activeListener, moveBatch);
        // A high-frequency tablet must not monopolize the JavaFX thread. Leave
        // remaining packets for the next pulse so buttons, layout and painting
        // continue to receive time even while the pen is hovering or drawing.
        if (!pendingStylusEvents.isEmpty() && stylusDrainScheduled.compareAndSet(false, true)) {
            Platform.runLater(this::drainStylusEvents);
        }
    }

    private boolean mousePrimaryButtonDown() {
        return nativePrimaryButtonDown || (javafxPrimaryButtonDown && javafxPrimaryPressStartedInsideTarget);
    }

    private boolean hasInkContactForPacket(StylusEvent event) {
        if (event == null) {
            return false;
        }
        if (event.getCursor() == StylusCursor.MOUSE) {
            return mousePrimaryButtonDown();
        }
        return hasInkContact(event, nativePrimaryButtonDown || isPrimaryButton(event));
    }

    private void finishActiveStroke(InkInputListener activeListener, StylusEvent event, String reason) {
        if (activeListener == null || !strokeActive) {
            clearStrokeState();
            return;
        }
        if (event == null && lastAcceptedTargetLocalPoint == null) {
            clearStrokeState();
            return;
        }
        InkInputSample sample;
        if (event != null) {
            sample = sampleFromStylusEvent(event, false, inputSourceForCurrentPacket());
        } else {
            Point2D point = lastAcceptedTargetLocalPoint;
            sample = new InkInputSample(
                    point.getX(),
                    point.getY(),
                    System.nanoTime(),
                    0.0,
                    lastInputCursor,
                    false,
                    lastInputCursor == InkInputCursor.ERASER,
                    lastInputRawPressure,
                    inputSourceForCurrentPacket());
        }
        activeListener.onStrokeEnd(sample);
        clearStrokeState();
        reportInputDiagnostic("Trazo LectureStudio finalizado: " + reason);
    }

    private void finishActiveStrokeFromState(String reason) {
        if (!strokeActive) {
            return;
        }
        finishActiveStroke(listener, null, reason);
    }

    private void clearStrokeState() {
        strokeActive = false;
        lastAcceptedTargetLocalPoint = null;
        // Keep the coordinate space learned for this target between strokes.
        // Native pen packets can beat the first JavaFX mouse event when focus
        // returns from a toolbar. Re-detecting here used the stale toolbar
        // anchor for exactly that first stroke and produced a visible offset.
        // attach/detach, layout/zoom changes and resetCoordinateState() remain
        // the explicit boundaries that invalidate the calibration.
    }

    private void clearPointerState(String reason) {
        nativePrimaryButtonDown = false;
        javafxPrimaryButtonDown = false;
        javafxPrimaryPressStartedInsideTarget = false;
        reportInputDiagnostic("Estado de boton primario limpiado: " + reason);
    }

    private static void flushMoveBatch(InkInputListener activeListener, List<InkInputSample> moveBatch) {
        if (moveBatch.isEmpty()) {
            return;
        }
        activeListener.onStrokeMoveBatch(List.copyOf(moveBatch));
        moveBatch.clear();
    }

    private boolean nativeRecentlyActive() {
        long last = lastNativeEventNanos.get();
        return last > 0L && System.nanoTime() - last < FALLBACK_SUPPRESSION_NANOS;
    }

    private static double safeCoordinate(double value) {
        return Double.isFinite(value) ? value : 0.0;
    }

    private static double normalizePressure(double pressure) {
        if (!Double.isFinite(pressure)) {
            return 1.0;
        }
        if (pressure <= 0.0) {
            return 0.0;
        }
        if (pressure <= 1.0) {
            return pressure;
        }
        double denominator = pressure <= 1024.0
                ? 1024.0
                : pressure <= 4096.0
                ? 4096.0
                : pressure <= 8192.0
                ? 8192.0
                : 65535.0;
        return Math.max(0.0, Math.min(1.0, pressure / denominator));
    }

    private void installNativeAttachHooks() {
        if (target == null || stylusListener == null) {
            return;
        }
        Scene scene = target.getScene();
        if (scene != null) {
            observeScene(scene);
            return;
        }
        sceneListener = observable -> {
            if (target == null || target.getScene() == null) {
                return;
            }
            target.sceneProperty().removeListener(sceneListener);
            sceneListener = null;
            observeScene(target.getScene());
        };
        target.sceneProperty().addListener(sceneListener);
    }

    private void observeScene(Scene scene) {
        removeScenePrimaryButtonTracker();
        observedScene = scene;
        installScenePrimaryButtonTracker(scene);
        Window window = scene.getWindow();
        if (window != null) {
            observeWindow(window);
            return;
        }
        windowListener = observable -> {
            if (observedScene == null || observedScene.getWindow() == null) {
                return;
            }
            observedScene.windowProperty().removeListener(windowListener);
            windowListener = null;
            observeWindow(observedScene.getWindow());
        };
        scene.windowProperty().addListener(windowListener);
    }

    private void observeWindow(Window window) {
        removeWindowStateListeners();
        observedWindow = window;
        showingListener = (observable, oldValue, showing) -> {
            if (observedWindow == null) {
                return;
            }
            if (Boolean.TRUE.equals(showing)) {
                attachNative(observedWindow);
            } else {
                finishActiveStrokeFromState("ventana oculta");
                clearPointerState("ventana oculta");
            }
        };
        window.showingProperty().addListener(showingListener);
        focusedListener = (observable, oldValue, focused) -> {
            if (!Boolean.TRUE.equals(focused)) {
                finishActiveStrokeFromState("ventana sin foco");
                clearPointerState("ventana sin foco");
            }
        };
        window.focusedProperty().addListener(focusedListener);
        if (window.isShowing()) {
            attachNative(window);
        }
    }

    private void attachNative(Window window) {
        if (nativeAttached || window == null || target == null || stylusListener == null) {
            return;
        }
        try {
            OptionalLong hwnd = WindowsNativeWindowHandleResolver.resolveHandle(target);
            if (hwnd.isEmpty()) {
                fallbackReason = "LectureStudio stylus no pudo engancharse: no se encontro HWND nativo de la ventana.";
                nativeAttachFailed = true;
                reportInputDiagnostic(fallbackReason);
                return;
            }
            stylusManager = StylusManager.getInstance();
            attachedWindowHandle = hwnd.getAsLong();
            stylusManager.attachStylusListener(stylusListener, Long.valueOf(attachedWindowHandle));
            stylusManager.enableStylusListener(stylusListener, true);
            attachedWindow = window;
            nativeAttached = true;
            nativeAttachFailed = false;
            fallbackReason = waitingReason(strict);
            reportInputDiagnostic("LectureStudio enganchado por HWND " + attachedWindowHandle + "; listener nativo habilitado.");
        } catch (Throwable ex) {
            if (isFatal(ex)) {
                throw ex;
            }
            fallbackReason = "LectureStudio stylus no pudo engancharse: " + ex.getClass().getSimpleName()
                    + (ex.getMessage() == null ? "" : " - " + ex.getMessage());
            clearNativeAttachHooks();
            stylusListener = null;
            nativeAttached = false;
            nativeAttachFailed = true;
            attachedWindow = null;
            attachedWindowHandle = null;
            reportInputDiagnostic(fallbackReason);
        }
    }

    private void clearNativeAttachHooks() {
        removeScenePrimaryButtonTracker();
        if (target != null && sceneListener != null) {
            target.sceneProperty().removeListener(sceneListener);
        }
        if (observedScene != null && windowListener != null) {
            observedScene.windowProperty().removeListener(windowListener);
        }
        removeWindowStateListeners();
        sceneListener = null;
        windowListener = null;
    }

    private void removeWindowStateListeners() {
        if (observedWindow != null && showingListener != null) {
            observedWindow.showingProperty().removeListener(showingListener);
        }
        if (observedWindow != null && focusedListener != null) {
            observedWindow.focusedProperty().removeListener(focusedListener);
        }
        showingListener = null;
        focusedListener = null;
    }

    private void installCoordinateAnchor(Node target) {
        coordinateAnchorHandler = event -> {
            if (event != null) {
                coordinateResolver.recordJavaFxAnchor(target, event.getSceneX(), event.getSceneY());
            }
        };
        target.addEventFilter(MouseEvent.MOUSE_MOVED, coordinateAnchorHandler);
        target.addEventFilter(MouseEvent.MOUSE_DRAGGED, coordinateAnchorHandler);
        target.addEventFilter(MouseEvent.MOUSE_PRESSED, coordinateAnchorHandler);
        target.addEventFilter(MouseEvent.MOUSE_RELEASED, coordinateAnchorHandler);
    }

    private void removeCoordinateAnchor() {
        if (target == null || coordinateAnchorHandler == null) {
            coordinateAnchorHandler = null;
            return;
        }
        target.removeEventFilter(MouseEvent.MOUSE_MOVED, coordinateAnchorHandler);
        target.removeEventFilter(MouseEvent.MOUSE_DRAGGED, coordinateAnchorHandler);
        target.removeEventFilter(MouseEvent.MOUSE_PRESSED, coordinateAnchorHandler);
        target.removeEventFilter(MouseEvent.MOUSE_RELEASED, coordinateAnchorHandler);
        coordinateAnchorHandler = null;
    }

    private void installScenePrimaryButtonTracker(Scene scene) {
        removeScenePrimaryButtonTracker();
        if (scene == null) {
            return;
        }
        scenePrimaryButtonHandler = event -> {
            if (event == null) {
                return;
            }
            boolean primaryPress = event.getEventType() == MouseEvent.MOUSE_PRESSED
                    && event.getButton() == MouseButton.PRIMARY;
            boolean primaryRelease = event.getEventType() == MouseEvent.MOUSE_RELEASED
                    && event.getButton() == MouseButton.PRIMARY;
            if (primaryPress) {
                javafxPrimaryButtonDown = true;
                javafxPrimaryPressStartedInsideTarget = scenePointInsideTarget(event.getSceneX(), event.getSceneY());
                return;
            }
            if (primaryRelease || !event.isPrimaryButtonDown()) {
                boolean hadMouseContact = lastInputCursor == InkInputCursor.MOUSE
                        && (javafxPrimaryButtonDown || nativePrimaryButtonDown || strokeActive);
                clearPointerState(primaryRelease ? "javafx primary release" : "javafx sin boton primario");
                if (hadMouseContact) {
                    finishActiveStrokeFromState(primaryRelease ? "javafx primary release" : "javafx sin boton primario");
                }
                return;
            }
            javafxPrimaryButtonDown = event.isPrimaryButtonDown();
        };
        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, scenePrimaryButtonHandler);
        scene.addEventFilter(MouseEvent.MOUSE_DRAGGED, scenePrimaryButtonHandler);
        scene.addEventFilter(MouseEvent.MOUSE_RELEASED, scenePrimaryButtonHandler);
        scene.addEventFilter(MouseEvent.MOUSE_MOVED, scenePrimaryButtonHandler);
    }

    private void removeScenePrimaryButtonTracker() {
        if (observedScene == null || scenePrimaryButtonHandler == null) {
            scenePrimaryButtonHandler = null;
            return;
        }
        observedScene.removeEventFilter(MouseEvent.MOUSE_PRESSED, scenePrimaryButtonHandler);
        observedScene.removeEventFilter(MouseEvent.MOUSE_DRAGGED, scenePrimaryButtonHandler);
        observedScene.removeEventFilter(MouseEvent.MOUSE_RELEASED, scenePrimaryButtonHandler);
        observedScene.removeEventFilter(MouseEvent.MOUSE_MOVED, scenePrimaryButtonHandler);
        scenePrimaryButtonHandler = null;
    }

    private boolean scenePointInsideTarget(double sceneX, double sceneY) {
        if (target == null || !Double.isFinite(sceneX) || !Double.isFinite(sceneY)) {
            return false;
        }
        Point2D targetPoint = target.sceneToLocal(sceneX, sceneY);
        return insideTargetBounds(targetPoint);
    }

    private boolean translateToTarget(StylusEvent event, InkInputCursor cursor) {
        if (event == null || target == null || event.getAxesData() == null) {
            return false;
        }
        StylusAxesData axes = event.getAxesData();
        double rawX = safeCoordinate(axes.getX());
        double rawY = safeCoordinate(axes.getY());
        NativeInkCoordinateSpaceResolver.TranslationResult result =
                coordinateResolver.translate(target, rawX, rawY, strokeActive, cursor);
        if (!result.accepted()) {
            reportInputDiagnostic("Evento LectureStudio rechazado: raw=("
                    + format(rawX) + "," + format(rawY) + ") razon=" + result.rejectionReason()
                    + " coords=" + coordinateResolver.lockedSpaceLabel());
            return false;
        }
        Point2D translated = result.point();
        event.translate(translated.getX(), translated.getY());
        lastAcceptedTargetLocalPoint = translated;
        lastCoordinateSpaceLabel = result.diagnosticLabel();
        lastInputCursor = cursor;
        lastInputRawPressure = event.getAxesData().getPressure();
        if (INK_INPUT_DIAGNOSTICS) {
            reportInputDiagnostic("Evento LectureStudio raw=(" + format(rawX) + "," + format(rawY)
                    + ") target=(" + format(translated.getX()) + "," + format(translated.getY())
                    + ") coords=" + lastCoordinateSpaceLabel
                    + " cursor=" + cursor
                    + " contacto=" + hasInkContactForPacket(event)
                    + (result.lockedNow() ? " bloqueado" : ""));
        }
        return true;
    }

    private boolean eventInsideTarget(StylusEvent event) {
        if (target == null || event == null || event.getAxesData() == null) {
            return false;
        }
        StylusAxesData axes = event.getAxesData();
        return insideTargetBounds(new Point2D(axes.getX(), axes.getY()));
    }

    private boolean insideTargetBounds(Point2D point) {
        return point != null
                && target != null
                && target.getLayoutBounds().contains(point.getX(), point.getY());
    }

    private String inputSourceForCurrentPacket() {
        return "LectureStudio coords " + lastCoordinateSpaceLabel;
    }

    private static InkInputCursor cursorFromStylusEvent(StylusEvent event) {
        if (event == null) {
            return InkInputCursor.UNKNOWN;
        }
        return switch (event.getCursor()) {
            case ERASER -> InkInputCursor.ERASER;
            case MOUSE -> InkInputCursor.MOUSE;
            case PEN -> InkInputCursor.PEN;
            default -> InkInputCursor.UNKNOWN;
        };
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static boolean isFatal(Throwable ex) {
        return ex instanceof VirtualMachineError || ex instanceof ThreadDeath;
    }

    private static Optional<String> stylusModuleNativeResourceFailure() {
        if (!System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")) {
            return Optional.of("LectureStudio stylus solo esta habilitado para Windows x86_64 en esta version.");
        }
        try (InputStream input = StylusManager.class.getResourceAsStream("/" + NATIVE_LIBRARY_RESOURCE)) {
            if (input == null) {
                return Optional.of("No se encontro " + NATIVE_LIBRARY_RESOURCE
                        + " como recurso del modulo stylus. Ejecuta con --patch-module=stylus=<stylus-windows-x86_64.jar>.");
            }
            reportInputDiagnostic("stylus.dll visible desde el modulo stylus.");
            return Optional.empty();
        } catch (IOException | RuntimeException ex) {
            return Optional.of("No se pudo verificar " + NATIVE_LIBRARY_RESOURCE + ": " + describe(ex));
        }
    }

    private static InkInputProvider unavailable(String reason) {
        reportInputDiagnostic(reason);
        return new UnavailableInkInputProvider(InkInputCapabilities.lectureStudioUnavailable(reason));
    }

    private static String waitingReason(boolean strict) {
        return strict
                ? "LectureStudio stylus configurado; esperando eventos nativos reales de la tableta."
                : "LectureStudio stylus instalado, esperando eventos nativos reales. "
                + "Si no llegan, se usa el siguiente proveedor de entrada.";
    }

    private static String describe(Throwable ex) {
        return ex.getClass().getSimpleName() + (ex.getMessage() == null ? "" : " - " + ex.getMessage());
    }

    private static void reportInputDiagnostic(String message) {
        if (INK_INPUT_DIAGNOSTICS) {
            LOGGER.debug("LectureStudio stylus: {}", message);
        }
    }

    private final class NativeStylusListener implements StylusListener {
        @SuppressWarnings("unused")
        private long nativeHandle;

        @Override
        public void onCursorChange(StylusEvent event) {
            enqueueStylusEvent(StylusEventKind.CURSOR_CHANGE, event);
        }

        @Override
        public void onCursorMove(StylusEvent event) {
            enqueueStylusEvent(StylusEventKind.CURSOR_MOVE, event);
        }

        @Override
        public void onButtonDown(StylusEvent event) {
            enqueueStylusEvent(StylusEventKind.BUTTON_DOWN, event);
        }

        @Override
        public void onButtonUp(StylusEvent event) {
            enqueueStylusEvent(StylusEventKind.BUTTON_UP, event);
        }
    }

    private enum StylusEventKind {
        CURSOR_CHANGE,
        CURSOR_MOVE,
        BUTTON_DOWN,
        BUTTON_UP
    }

    private record PendingStylusEvent(StylusEventKind kind, StylusEvent event) {
    }

    private final class SuppressingFallbackListener implements InkInputListener {
        private final InkInputListener delegate;

        private SuppressingFallbackListener(InkInputListener delegate) {
            this.delegate = delegate;
        }

        @Override
        public void onHover(InkInputSample sample) {
            if (!nativeRecentlyActive()) {
                delegate.onHover(sample);
            }
        }

        @Override
        public boolean onStrokeStart(InkInputSample sample) {
            return !nativeRecentlyActive() && delegate.onStrokeStart(sample);
        }

        @Override
        public boolean onStrokeMove(InkInputSample sample) {
            return !nativeRecentlyActive() && delegate.onStrokeMove(sample);
        }

        @Override
        public boolean onStrokeMoveBatch(java.util.List<InkInputSample> samples) {
            return !nativeRecentlyActive() && delegate.onStrokeMoveBatch(samples);
        }

        @Override
        public boolean onStrokeEnd(InkInputSample sample) {
            return !nativeRecentlyActive() && delegate.onStrokeEnd(sample);
        }
    }
}

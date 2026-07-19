package com.marcosmoreiradev.docupodcaststudio.presentation.ink.input;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.LPARAM;
import com.sun.jna.platform.win32.WinDef.LRESULT;
import com.sun.jna.platform.win32.WinDef.POINT;
import com.sun.jna.platform.win32.WinDef.RECT;
import com.sun.jna.platform.win32.WinDef.WPARAM;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.StdCallLibrary.StdCallCallback;
import com.sun.jna.win32.W32APIOptions;
import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Windows Pointer / Windows Ink input provider.
 *
 * <p>This class is the only place that talks to Win32/JNA for stylus input. UI
 * code receives normalized {@link InkInputSample} values through
 * {@link InkInputProvider} and never imports JNA directly.</p>
 */
public final class WindowsPointerInkInputProvider implements InkInputProvider {
    private static final String DISABLE_PROPERTY = "docupodcast.ink.disableWindowsPointer";
    private static final String DIAGNOSTICS_PROPERTY = "docupodcast.ink.inputDiagnostics";
    private static final String WINDOWS = "win";
    private static final int GWL_WNDPROC = -4;
    private static final int WM_POINTERUPDATE = 0x0245;
    private static final int WM_POINTERDOWN = 0x0246;
    private static final int WM_POINTERUP = 0x0247;
    private static final int WM_POINTERLEAVE = 0x024A;
    private static final int WM_POINTERCAPTURECHANGED = 0x024C;
    private static final int PEN_FLAG_ERASER = 0x00000004;
    private static final int POINTER_FLAG_INCONTACT = 0x00000004;
    private static final int POINTER_FLAG_PRIMARY = 0x00002000;
    private static final int PT_PEN = 0x00000003;
    private static final int MAX_HISTORY_ENTRIES = 128;
    private static final long FALLBACK_SUPPRESSION_NANOS = 150_000_000L;

    private final InkInputProvider fallback;
    private final boolean nativeOnly;
    private final Queue<NativePacket> pendingPackets = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean drainScheduled = new AtomicBoolean(false);
    private final AtomicLong lastNativePacketNanos = new AtomicLong(0L);
    private final AtomicBoolean nativePacketsSeen = new AtomicBoolean(false);

    private Node target;
    private InkInputListener listener;
    private Scene observedScene;
    private Window observedWindow;
    private InvalidationListener sceneListener;
    private InvalidationListener windowListener;
    private ChangeListener<Boolean> showingListener;
    private HWND hwnd;
    private Pointer originalWndProc;
    private WindowProc windowProc;
    private boolean hookInstalled;
    private boolean pointerTargetRegistered;
    private String fallbackReason = "Windows Pointer todavia no esta conectado a esta ventana.";

    private WindowsPointerInkInputProvider() {
        this(new JavaFxMouseInputProvider());
    }

    private WindowsPointerInkInputProvider(InkInputProvider fallback) {
        this.fallback = fallback == null ? NoopInkInputProvider.INSTANCE : fallback;
        this.nativeOnly = this.fallback == NoopInkInputProvider.INSTANCE;
    }

    public static Optional<InkInputProvider> tryCreate() {
        if (Boolean.getBoolean(DISABLE_PROPERTY) || !isWindows()) {
            return Optional.empty();
        }
        return Optional.of(new WindowsPointerInkInputProvider());
    }

    public static Optional<InkInputProvider> tryCreate(InkInputProvider fallback) {
        if (Boolean.getBoolean(DISABLE_PROPERTY) || !isWindows()) {
            return Optional.empty();
        }
        return Optional.of(new WindowsPointerInkInputProvider(fallback));
    }

    @Override
    public InkInputCapabilities capabilities() {
        if (nativePacketsSeen.get()) {
            return InkInputCapabilities.windowsPointer();
        }
        if (nativeOnly) {
            return InkInputCapabilities.windowsPointerWaiting(fallbackReason);
        }
        InkInputCapabilities fallbackCapabilities = fallback.capabilities();
        if (!"JavaFX mouse".equals(fallbackCapabilities.providerName())) {
            return fallbackCapabilities;
        }
        if (hookInstalled) {
            return InkInputCapabilities.javafxMouse(
                    "Windows Pointer instalado, esperando paquetes reales del lapiz. "
                            + "Si la tableta no publica Windows Ink, se usa JavaFX mouse.");
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
        fallback.attach(target, new SuppressingFallbackListener(listener));
        installNativeHookWhenReady();
    }

    @Override
    public void detach() {
        uninstallNativeHook();
        fallback.detach();
        pendingPackets.clear();
        drainScheduled.set(false);
        target = null;
        listener = null;
    }

    @Override
    public void resetCoordinateState() {
        fallback.resetCoordinateState();
    }

    private void installNativeHook() {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(this::installNativeHook);
            return;
        }
        try {
            Optional<HWND> resolved = WindowsHwndResolver.resolve(target);
            if (resolved.isEmpty()) {
                fallbackReason = "No se pudo resolver HWND de la ventana JavaFX.";
                return;
            }
            hwnd = resolved.get();
            windowProc = this::handleWindowMessage;
            originalWndProc = User32PointerApi.INSTANCE.SetWindowLongPtr(hwnd, GWL_WNDPROC, windowProc);
            if (originalWndProc == null || Pointer.nativeValue(originalWndProc) == 0L) {
                fallbackReason = "Win32 no permitio instalar WNDPROC para Windows Pointer.";
                windowProc = null;
                hwnd = null;
                return;
            }
            hookInstalled = true;
            pointerTargetRegistered = registerPointerTarget(hwnd);
            fallbackReason = "";
            reportDiagnostic("Hook instalado. Registro PT_PEN=" + pointerTargetRegistered);
        } catch (RuntimeException | LinkageError ex) {
            fallbackReason = "Windows Pointer no disponible: " + ex.getClass().getSimpleName()
                    + (ex.getMessage() == null ? "" : " - " + ex.getMessage());
            reportDiagnostic(fallbackReason);
            hookInstalled = false;
            windowProc = null;
            hwnd = null;
            originalWndProc = null;
            pointerTargetRegistered = false;
        }
    }

    private void uninstallNativeHook() {
        clearNativeHookReadinessListeners();
        if (pointerTargetRegistered && hwnd != null) {
            try {
                User32PointerApi.INSTANCE.UnregisterPointerInputTarget(hwnd, PT_PEN);
            } catch (RuntimeException ignored) {
                // Best effort during dialog close.
            }
        }
        if (hookInstalled && hwnd != null && originalWndProc != null) {
            try {
                User32PointerApi.INSTANCE.SetWindowLongPtr(hwnd, GWL_WNDPROC, originalWndProc);
            } catch (RuntimeException ignored) {
                // Best effort during dialog close.
            }
        }
        hookInstalled = false;
        pointerTargetRegistered = false;
        hwnd = null;
        originalWndProc = null;
        windowProc = null;
    }

    private static boolean registerPointerTarget(HWND hwnd) {
        if (hwnd == null) {
            return false;
        }
        try {
            return User32PointerApi.INSTANCE.RegisterPointerInputTarget(hwnd, PT_PEN);
        } catch (RuntimeException | LinkageError ex) {
            return false;
        }
    }

    private void installNativeHookWhenReady() {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(this::installNativeHookWhenReady);
            return;
        }
        if (target == null) {
            return;
        }
        Scene scene = target.getScene();
        if (scene == null) {
            observeScene();
            fallbackReason = "Windows Pointer esperando que el lienzo entre en una escena JavaFX.";
            return;
        }
        observedScene = scene;
        Window window = scene.getWindow();
        if (window == null) {
            observeWindow(scene);
            fallbackReason = "Windows Pointer esperando la ventana JavaFX del dialogo.";
            return;
        }
        observedWindow = window;
        if (!window.isShowing()) {
            observeShowing(window);
            fallbackReason = "Windows Pointer esperando que la ventana sea visible.";
            return;
        }
        installNativeHook();
    }

    private void observeScene() {
        if (target == null || sceneListener != null) {
            return;
        }
        sceneListener = observable -> {
            if (target == null || target.getScene() == null) {
                return;
            }
            target.sceneProperty().removeListener(sceneListener);
            sceneListener = null;
            installNativeHookWhenReady();
        };
        target.sceneProperty().addListener(sceneListener);
    }

    private void observeWindow(Scene scene) {
        if (scene == null || windowListener != null) {
            return;
        }
        windowListener = observable -> {
            if (observedScene == null || observedScene.getWindow() == null) {
                return;
            }
            observedScene.windowProperty().removeListener(windowListener);
            windowListener = null;
            installNativeHookWhenReady();
        };
        scene.windowProperty().addListener(windowListener);
    }

    private void observeShowing(Window window) {
        if (window == null || showingListener != null) {
            return;
        }
        showingListener = (observable, oldValue, showing) -> {
            if (!Boolean.TRUE.equals(showing) || observedWindow == null) {
                return;
            }
            observedWindow.showingProperty().removeListener(showingListener);
            showingListener = null;
            installNativeHookWhenReady();
        };
        window.showingProperty().addListener(showingListener);
    }

    private void clearNativeHookReadinessListeners() {
        if (target != null && sceneListener != null) {
            target.sceneProperty().removeListener(sceneListener);
        }
        if (observedScene != null && windowListener != null) {
            observedScene.windowProperty().removeListener(windowListener);
        }
        if (observedWindow != null && showingListener != null) {
            observedWindow.showingProperty().removeListener(showingListener);
        }
        observedScene = null;
        observedWindow = null;
        sceneListener = null;
        windowListener = null;
        showingListener = null;
    }

    private LRESULT handleWindowMessage(HWND hWnd, int message, WPARAM wParam, LPARAM lParam) {
        if (isPointerMessage(message)) {
            int pointerId = pointerId(wParam);
            enqueuePointerPackets(message, pointerId);
        }
        return User32PointerApi.INSTANCE.CallWindowProc(originalWndProc, hWnd, message, wParam, lParam);
    }

    private void enqueuePointerPackets(int message, int pointerId) {
        List<PointerPenInfo> history = readPenHistory(pointerId);
        if (history.isEmpty()) {
            PointerPenInfo info = new PointerPenInfo();
            if (User32PointerApi.INSTANCE.GetPointerPenInfo(pointerId, info)) {
                info.read();
                history = List.of(info);
            }
        }
        if (history.isEmpty()) {
            return;
        }
        history = new ArrayList<>(history);
        history.sort(Comparator.comparingLong(info -> info.pointerInfo.performanceCount));
        for (int index = 0; index < history.size(); index++) {
            PointerAction action = actionFor(message, index, history.size());
            pendingPackets.add(toPacket(history.get(index), action));
        }
        scheduleDrain();
    }

    private List<PointerPenInfo> readPenHistory(int pointerId) {
        PointerPenInfo template = new PointerPenInfo();
        int structSize = template.size();
        IntByReference entriesCount = new IntByReference(MAX_HISTORY_ENTRIES);
        IntByReference pointerCount = new IntByReference(1);
        Memory memory = new Memory((long) structSize * MAX_HISTORY_ENTRIES);
        boolean ok = User32PointerApi.INSTANCE.GetPointerFramePenInfoHistory(pointerId, entriesCount, pointerCount, memory);
        if (!ok) {
            return List.of();
        }
        int total = Math.max(0, Math.min(MAX_HISTORY_ENTRIES, entriesCount.getValue() * Math.max(1, pointerCount.getValue())));
        List<PointerPenInfo> result = new ArrayList<>(total);
        for (int i = 0; i < total; i++) {
            PointerPenInfo info = new PointerPenInfo(memory.share((long) i * structSize));
            info.read();
            result.add(info);
        }
        return result;
    }

    private NativePacket toPacket(PointerPenInfo info, PointerAction action) {
        double rawPressure = info.pressure < 0 ? 1.0 : info.pressure;
        double pressure = normalizePressure(rawPressure);
        boolean eraser = (info.penFlags & PEN_FLAG_ERASER) != 0;
        boolean primary = (info.pointerInfo.pointerFlags & POINTER_FLAG_PRIMARY) != 0
                || (info.pointerInfo.pointerFlags & POINTER_FLAG_INCONTACT) != 0;
        POINT location = info.pointerInfo.ptPixelLocation;
        return new NativePacket(
                action,
                info.pointerInfo.pointerId,
                location.x,
                location.y,
                info.pointerInfo.performanceCount == 0 ? System.nanoTime() : info.pointerInfo.performanceCount,
                pressure,
                rawPressure,
                eraser,
                primary);
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

    private void scheduleDrain() {
        if (drainScheduled.compareAndSet(false, true)) {
            Platform.runLater(this::drainNativePackets);
        }
    }

    private void drainNativePackets() {
        drainScheduled.set(false);
        Node localTarget = target;
        InkInputListener localListener = listener;
        if (localTarget == null || localListener == null) {
            pendingPackets.clear();
            return;
        }
        List<InkInputSample> moveBatch = new ArrayList<>();
        NativePacket packet;
        while ((packet = pendingPackets.poll()) != null) {
            InkInputSample sample = toSample(localTarget, packet);
            if (sample == null) {
                continue;
            }
            if (nativePacketsSeen.compareAndSet(false, true)) {
                reportDiagnostic("Primer paquete recibido: pressureRaw="
                        + packet.rawPressure + ", pressure=" + packet.pressure);
            }
            lastNativePacketNanos.set(System.nanoTime());
            if (packet.action == PointerAction.MOVE) {
                moveBatch.add(sample);
                continue;
            }
            if (!moveBatch.isEmpty()) {
                localListener.onStrokeMoveBatch(moveBatch);
                moveBatch.clear();
            }
            if (packet.action == PointerAction.START) {
                localListener.onStrokeStart(sample);
            } else if (packet.action == PointerAction.END || packet.action == PointerAction.CANCEL) {
                localListener.onStrokeEnd(sample);
            } else if (packet.action == PointerAction.HOVER) {
                localListener.onHover(sample);
            }
        }
        if (!moveBatch.isEmpty()) {
            localListener.onStrokeMoveBatch(moveBatch);
        }
    }

    private InkInputSample toSample(Node localTarget, NativePacket packet) {
        Point2D local = localTarget.screenToLocal(packet.screenX, packet.screenY);
        if (local == null || !Double.isFinite(local.getX()) || !Double.isFinite(local.getY())) {
            return null;
        }
        InkInputCursor cursor = packet.eraser ? InkInputCursor.ERASER : InkInputCursor.PEN;
        return new InkInputSample(
                local.getX(),
                local.getY(),
                packet.nanos,
                packet.pressure,
                cursor,
                packet.primaryButtonDown,
                packet.eraser,
                packet.rawPressure,
                "Windows Pointer");
    }

    private boolean nativeRecentlyActive() {
        long last = lastNativePacketNanos.get();
        return last > 0L && System.nanoTime() - last < FALLBACK_SUPPRESSION_NANOS;
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains(WINDOWS);
    }

    private static boolean isPointerMessage(int message) {
        return message == WM_POINTERDOWN
                || message == WM_POINTERUPDATE
                || message == WM_POINTERUP
                || message == WM_POINTERLEAVE
                || message == WM_POINTERCAPTURECHANGED;
    }

    private static int pointerId(WPARAM wParam) {
        return wParam.intValue() & 0xFFFF;
    }

    private static void reportDiagnostic(String message) {
        if (Boolean.getBoolean(DIAGNOSTICS_PROPERTY)) {
            System.out.println("[WindowsPointerInk] " + message);
        }
    }

    private static PointerAction actionFor(int message, int index, int total) {
        if (message == WM_POINTERDOWN && index == 0) {
            return PointerAction.START;
        }
        if ((message == WM_POINTERUP || message == WM_POINTERLEAVE || message == WM_POINTERCAPTURECHANGED)
                && index == total - 1) {
            return message == WM_POINTERUP ? PointerAction.END : PointerAction.CANCEL;
        }
        return PointerAction.MOVE;
    }

    private enum PointerAction {
        START,
        MOVE,
        END,
        HOVER,
        CANCEL
    }

    private record NativePacket(PointerAction action,
                                int pointerId,
                                int screenX,
                                int screenY,
                                long nanos,
                                double pressure,
                                double rawPressure,
                                boolean eraser,
                                boolean primaryButtonDown) {
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
        public boolean onStrokeMoveBatch(List<InkInputSample> samples) {
            return !nativeRecentlyActive() && delegate.onStrokeMoveBatch(samples);
        }

        @Override
        public boolean onStrokeEnd(InkInputSample sample) {
            return !nativeRecentlyActive() && delegate.onStrokeEnd(sample);
        }
    }

    @Structure.FieldOrder({
            "pointerType",
            "pointerId",
            "frameId",
            "pointerFlags",
            "sourceDevice",
            "hwndTarget",
            "ptPixelLocation",
            "ptHimetricLocation",
            "ptPixelLocationRaw",
            "ptHimetricLocationRaw",
            "dwTime",
            "historyCount",
            "inputData",
            "keyStates",
            "performanceCount",
            "buttonChangeType"
    })
    public static class PointerInfo extends Structure {
        public int pointerType;
        public int pointerId;
        public int frameId;
        public int pointerFlags;
        public HANDLE sourceDevice;
        public HWND hwndTarget;
        public POINT ptPixelLocation;
        public POINT ptHimetricLocation;
        public POINT ptPixelLocationRaw;
        public POINT ptHimetricLocationRaw;
        public int dwTime;
        public int historyCount;
        public int inputData;
        public int keyStates;
        public long performanceCount;
        public int buttonChangeType;

        public PointerInfo() {
            super();
        }

        public PointerInfo(Pointer pointer) {
            super(pointer);
        }
    }

    @Structure.FieldOrder({
            "pointerInfo",
            "penFlags",
            "penMask",
            "pressure",
            "rotation",
            "tiltX",
            "tiltY"
    })
    public static class PointerPenInfo extends Structure {
        public PointerInfo pointerInfo;
        public int penFlags;
        public int penMask;
        public int pressure;
        public int rotation;
        public int tiltX;
        public int tiltY;

        public PointerPenInfo() {
            super();
        }

        public PointerPenInfo(Pointer pointer) {
            super(pointer);
        }
    }

    private interface WindowProc extends StdCallCallback {
        LRESULT callback(HWND hWnd, int message, WPARAM wParam, LPARAM lParam);
    }

    private interface User32PointerApi extends StdCallLibrary {
        User32PointerApi INSTANCE = Native.load("user32", User32PointerApi.class, W32APIOptions.DEFAULT_OPTIONS);

        Pointer SetWindowLongPtr(HWND hWnd, int nIndex, WindowProc callback);

        Pointer SetWindowLongPtr(HWND hWnd, int nIndex, Pointer pointer);

        LRESULT CallWindowProc(Pointer lpPrevWndFunc, HWND hWnd, int uMsg, WPARAM wParam, LPARAM lParam);

        boolean GetPointerPenInfo(int pointerId, PointerPenInfo penInfo);

        boolean GetPointerFramePenInfoHistory(int pointerId,
                                              IntByReference entriesCount,
                                              IntByReference pointerCount,
                                              Pointer penInfo);

        boolean RegisterPointerInputTarget(HWND hWnd, int pointerType);

        boolean UnregisterPointerInputTarget(HWND hWnd, int pointerType);

        boolean EnumWindows(com.sun.jna.platform.win32.WinUser.WNDENUMPROC lpEnumFunc, Pointer arg);

        int GetWindowThreadProcessId(HWND hWnd, IntByReference processId);

        boolean IsWindowVisible(HWND hWnd);

        int GetWindowTextW(HWND hWnd, char[] lpString, int nMaxCount);

        boolean GetWindowRect(HWND hWnd, RECT rect);
    }

    private static final class WindowsHwndResolver {
        private WindowsHwndResolver() {
        }

        private static Optional<HWND> resolve(Node node) {
            if (node == null || node.getScene() == null || node.getScene().getWindow() == null) {
                return Optional.empty();
            }
            Window window = node.getScene().getWindow();
            long currentPid = ProcessHandle.current().pid();
            String title = window instanceof Stage stage ? stage.getTitle() : "";
            WindowBounds windowBounds = new WindowBounds(
                    window.getX(),
                    window.getY(),
                    window.getWidth(),
                    window.getHeight());
            List<HWND> candidates = new ArrayList<>();
            User32PointerApi.INSTANCE.EnumWindows((hWnd, data) -> {
                if (!User32PointerApi.INSTANCE.IsWindowVisible(hWnd)) {
                    return true;
                }
                IntByReference pid = new IntByReference();
                User32PointerApi.INSTANCE.GetWindowThreadProcessId(hWnd, pid);
                if (pid.getValue() != (int) currentPid) {
                    return true;
                }
                if (!title.isBlank() && windowTitle(hWnd).contains(title)) {
                    candidates.add(hWnd);
                    return false;
                }
                if (roughlySameBounds(hWnd, windowBounds)) {
                    candidates.add(hWnd);
                }
                return true;
            }, null);
            return candidates.stream().findFirst();
        }

        private static String windowTitle(HWND hWnd) {
            char[] buffer = new char[512];
            int length = User32PointerApi.INSTANCE.GetWindowTextW(hWnd, buffer, buffer.length);
            return length <= 0 ? "" : Native.toString(buffer);
        }

        private static boolean roughlySameBounds(HWND hWnd, WindowBounds bounds) {
            RECT rect = new RECT();
            if (!User32PointerApi.INSTANCE.GetWindowRect(hWnd, rect)) {
                return false;
            }
            double width = rect.right - rect.left;
            double height = rect.bottom - rect.top;
            return Math.abs(rect.left - bounds.x()) < 24
                    && Math.abs(rect.top - bounds.y()) < 48
                    && Math.abs(width - bounds.width()) < 64
                    && Math.abs(height - bounds.height()) < 64;
        }

        private record WindowBounds(double x, double y, double width, double height) {
        }
    }
}

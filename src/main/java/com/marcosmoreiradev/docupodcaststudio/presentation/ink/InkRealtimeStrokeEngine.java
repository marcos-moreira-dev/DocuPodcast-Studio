package com.marcosmoreiradev.docupodcaststudio.presentation.ink;

import javafx.animation.AnimationTimer;
import javafx.scene.paint.Color;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Reusable low-latency ink engine for JavaFX surfaces.
 *
 * <p>Pointer handlers enqueue raw points only. Preview rendering is drained by
 * an AnimationTimer with a small frame budget, and final editable strokes are
 * committed separately.
 */
public final class InkRealtimeStrokeEngine {
    public static final boolean INK_PERF_DIAGNOSTICS =
            Boolean.getBoolean("docupodcast.ink.perfDiagnostics");
    public static final boolean FAST_INK_DEBUG =
            Boolean.getBoolean("docupodcast.ink.fastDebug");
    public static final boolean INK_INPUT_DIAGNOSTICS =
            Boolean.getBoolean("docupodcast.ink.inputDiagnostics");
    public static final boolean INK_RENDER_DIAGNOSTICS =
            Boolean.getBoolean("docupodcast.ink.renderDiagnostics");

    private static final double MIN_SAMPLE_DISTANCE = 0.35;
    private static final long FRAME_BUDGET_NANOS = 8_000_000L;
    private static final long INPUT_DRAIN_BUDGET_NANOS = 5_000_000L;
    private static final int MAX_POINTS_PER_FRAME = 512;
    private static final int MAX_INPUT_DRAIN_POINTS = 512;

    private final Sink sink;
    private final Deque<RawPoint> pendingPoints = new ArrayDeque<>();
    private final List<CommittedPoint> activePoints = new ArrayList<>();
    private final AnimationTimer timer;

    private boolean active;
    private boolean erase;
    private Color color = Color.BLACK;
    private double width = 3.0;
    private double pressure = 1.0;
    private double lastWidth = 3.0;
    private double activeMaxWidth = 3.0;
    private double lastX;
    private double lastY;
    private double lastMidX;
    private double lastMidY;
    private boolean hasMidpoint;
    private boolean hasQueuedPoint;
    private double lastQueuedX;
    private double lastQueuedY;
    private int diagnosticsQueued;
    private int diagnosticsDropped;
    private int diagnosticsPeak;
    private long diagnosticsLastLog;

    public InkRealtimeStrokeEngine(Sink sink) {
        this.sink = sink == null ? Sink.noop() : sink;
        this.timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                drainFrame(now);
            }
        };
    }

    public void start() {
        timer.start();
    }

    public void stop() {
        flushAll();
        timer.stop();
        pendingPoints.clear();
        activePoints.clear();
        active = false;
        hasQueuedPoint = false;
    }

    public void begin(double x, double y, long nanos, Color color, double width, boolean erase, double pressure) {
        pendingPoints.addLast(RawPoint.start(x, y, safeNanos(nanos), safeColor(color), safeWidth(width), erase, safePressure(pressure)));
        hasQueuedPoint = true;
        lastQueuedX = x;
        lastQueuedY = y;
        diagnosticsQueued++;
        diagnosticsPeak = Math.max(diagnosticsPeak, pendingPoints.size());
        drainInput();
    }

    public void move(double x, double y, long nanos, Color color, double width, boolean erase, double pressure) {
        if (!hasQueuedPoint) {
            begin(x, y, nanos, color, width, erase, pressure);
            return;
        }
        double distance = Math.hypot(x - lastQueuedX, y - lastQueuedY);
        if (distance < MIN_SAMPLE_DISTANCE) {
            diagnosticsDropped++;
            return;
        }
        pendingPoints.addLast(RawPoint.move(x, y, safeNanos(nanos), safeColor(color), safeWidth(width), erase, safePressure(pressure)));
        lastQueuedX = x;
        lastQueuedY = y;
        diagnosticsQueued++;
        diagnosticsPeak = Math.max(diagnosticsPeak, pendingPoints.size());
        drainInput();
    }

    public void end(double x, double y, long nanos, Color color, double width, boolean erase, double pressure) {
        pendingPoints.addLast(RawPoint.end(x, y, safeNanos(nanos), safeColor(color), safeWidth(width), erase, safePressure(pressure)));
        hasQueuedPoint = false;
        diagnosticsQueued++;
        diagnosticsPeak = Math.max(diagnosticsPeak, pendingPoints.size());
        drainInput();
    }

    public void flushAll() {
        int drained = 0;
        while (!pendingPoints.isEmpty()) {
            drainOne(pendingPoints.removeFirst());
            drained++;
        }
        commitActiveStroke();
        reportDiagnostics(drained, 0L);
    }

    private void drainFrame(long now) {
        int drained = drainPending(now + FRAME_BUDGET_NANOS, MAX_POINTS_PER_FRAME);
        reportDiagnostics(drained, Math.max(0L, System.nanoTime() - now));
    }

    private void drainInput() {
        long now = System.nanoTime();
        int drained = drainPending(now + INPUT_DRAIN_BUDGET_NANOS, MAX_INPUT_DRAIN_POINTS);
        reportDiagnostics(drained, Math.max(0L, System.nanoTime() - now));
    }

    private int drainPending(long deadline, int maxPoints) {
        int drained = 0;
        while (!pendingPoints.isEmpty() && drained < maxPoints && System.nanoTime() < deadline) {
            drainOne(pendingPoints.removeFirst());
            drained++;
        }
        return drained;
    }

    private void drainOne(RawPoint point) {
        boolean accepted = sink.acceptsPoint(point.x(), point.y());
        if (!accepted) {
            if (point.end()) {
                commitActiveStroke();
            }
            return;
        }
        if (point.start()
                || !active
                || point.erase() != erase
                || !sameColor(point.color(), color)) {
            commitActiveStroke();
            beginActiveStroke(point);
        } else {
            appendPoint(point);
        }
        if (point.end()) {
            commitActiveStroke();
        }
    }

    private void beginActiveStroke(RawPoint point) {
        active = true;
        erase = point.erase();
        color = safeColor(point.color());
        width = safeWidth(point.width());
        pressure = safePressure(point.pressure());
        lastWidth = InkBrushMath.pressureWidth(width, pressure);
        activeMaxWidth = width;
        activePoints.clear();
        sink.beginLiveStroke();
        lastX = point.x();
        lastY = point.y();
        lastMidX = point.x();
        lastMidY = point.y();
        hasMidpoint = false;
        activePoints.add(new CommittedPoint(point.x(), point.y(), point.nanos(), pressure));
        sink.previewLine(point.x(), point.y(), point.x() + 0.01, point.y() + 0.01, color, lastWidth, erase);
    }

    private void appendPoint(RawPoint point) {
        width = safeWidth(point.width());
        pressure = safePressure(point.pressure());
        activeMaxWidth = Math.max(activeMaxWidth, width);
        activePoints.add(new CommittedPoint(point.x(), point.y(), point.nanos(), pressure));
        previewSegment(point.x(), point.y(), InkBrushMath.pressureWidth(width, pressure));
    }

    private void previewSegment(double x, double y, double targetWidth) {
        width = targetWidth;
        sink.previewLine(lastX, lastY, x, y, color, width, erase);
        lastX = x;
        lastY = y;
        lastWidth = targetWidth;
    }

    private void previewStep(double x, double y) {
        double midX = (lastX + x) / 2.0;
        double midY = (lastY + y) / 2.0;
        if (!hasMidpoint) {
            sink.previewLine(lastX, lastY, midX, midY, color, width, erase);
        } else {
            sink.previewQuadratic(lastMidX, lastMidY, lastX, lastY, midX, midY, color, width, erase);
        }
        lastMidX = midX;
        lastMidY = midY;
        lastX = x;
        lastY = y;
        hasMidpoint = true;
    }

    private void commitActiveStroke() {
        if (!active) {
            return;
        }
        if (!activePoints.isEmpty()) {
            sink.commitStroke(new CommittedStroke(erase, color, activeMaxWidth, pressure, List.copyOf(activePoints)));
        }
        active = false;
        hasMidpoint = false;
        activePoints.clear();
    }

    private void reportDiagnostics(int drained, long frameNanos) {
        if (!INK_PERF_DIAGNOSTICS && !FAST_INK_DEBUG && !INK_INPUT_DIAGNOSTICS && !INK_RENDER_DIAGNOSTICS) {
            return;
        }
        long now = System.nanoTime();
        if (diagnosticsLastLog != 0 && now - diagnosticsLastLog < 1_000_000_000L) {
            return;
        }
        diagnosticsLastLog = now;
        System.out.println("[InkRealtime] pointsPerSecond=" + diagnosticsQueued
                + " pending=" + pendingPoints.size()
                + " peak=" + diagnosticsPeak
                + " dropped=" + diagnosticsDropped
                + " drained=" + drained
                + " frameMs=" + String.format(java.util.Locale.ROOT, "%.2f", frameNanos / 1_000_000.0));
        diagnosticsQueued = 0;
        diagnosticsDropped = 0;
        diagnosticsPeak = pendingPoints.size();
    }

    private static long safeNanos(long nanos) {
        return nanos > 0 ? nanos : System.nanoTime();
    }

    private static Color safeColor(Color color) {
        return color == null ? Color.BLACK : color;
    }

    private static double safeWidth(double width) {
        return InkBrushMath.safeMaxWidth(width);
    }

    private static double safePressure(double pressure) {
        return InkBrushMath.safePressure(pressure);
    }

    private static boolean sameColor(Color left, Color right) {
        Color safeLeft = safeColor(left);
        Color safeRight = safeColor(right);
        return Math.abs(safeLeft.getRed() - safeRight.getRed()) < 0.001
                && Math.abs(safeLeft.getGreen() - safeRight.getGreen()) < 0.001
                && Math.abs(safeLeft.getBlue() - safeRight.getBlue()) < 0.001
                && Math.abs(safeLeft.getOpacity() - safeRight.getOpacity()) < 0.001;
    }

    public interface Sink {
        void beginLiveStroke();

        void previewLine(double x1, double y1, double x2, double y2, Color color, double width, boolean erase);

        void previewQuadratic(double startX, double startY, double controlX, double controlY,
                              double endX, double endY, Color color, double width, boolean erase);

        void commitStroke(CommittedStroke stroke);

        default boolean acceptsPoint(double x, double y) {
            return true;
        }

        static Sink noop() {
            return new Sink() {
                @Override
                public void beginLiveStroke() {
                }

                @Override
                public void previewLine(double x1, double y1, double x2, double y2, Color color, double width, boolean erase) {
                }

                @Override
                public void previewQuadratic(double startX, double startY, double controlX, double controlY,
                                             double endX, double endY, Color color, double width, boolean erase) {
                }

                @Override
                public void commitStroke(CommittedStroke stroke) {
                }
            };
        }
    }

    public record CommittedStroke(boolean erase, Color color, double width, double pressure,
                                  List<CommittedPoint> points) {
        public CommittedStroke {
            color = safeColor(color);
            width = safeWidth(width);
            pressure = safePressure(pressure);
            points = points == null ? List.of() : List.copyOf(points);
        }
    }

    public record CommittedPoint(double x, double y, long nanos, double pressure) {
        public CommittedPoint {
            pressure = safePressure(pressure);
        }
    }

    private record RawPoint(double x, double y, long nanos, Color color, double width,
                            boolean erase, double pressure, boolean start, boolean end) {
        private static RawPoint start(double x, double y, long nanos, Color color, double width,
                                      boolean erase, double pressure) {
            return new RawPoint(x, y, nanos, color, width, erase, pressure, true, false);
        }

        private static RawPoint move(double x, double y, long nanos, Color color, double width,
                                     boolean erase, double pressure) {
            return new RawPoint(x, y, nanos, color, width, erase, pressure, false, false);
        }

        private static RawPoint end(double x, double y, long nanos, Color color, double width,
                                    boolean erase, double pressure) {
            return new RawPoint(x, y, nanos, color, width, erase, pressure, false, true);
        }
    }
}

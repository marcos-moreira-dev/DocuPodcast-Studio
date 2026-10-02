package com.marcosmoreiradev.docupodcaststudio.ink.geometry;

import com.marcosmoreiradev.docupodcaststudio.ink.model.InkPoint;
import com.marcosmoreiradev.docupodcaststudio.ink.model.InkStroke;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Deterministic geometry used by canvas selection and fill tools.
 *
 * <p>This class deliberately has no JavaFX or AI dependency. It calculates a contour safety band
 * from the stroke itself, so an imprecise pointer cannot select or fill several centimetres away
 * from the user's drawing. Open contours (for example a ghost drawn with an open base) are never
 * treated as fillable regions.</p>
 */
public final class InkSelectionGeometry {
    private static final double MIN_SAFETY_BAND = 4.0;
    private static final double MAX_SAFETY_BAND = 32.0;
    private static final double MIN_CLOSED_POINTS = 3;

    private InkSelectionGeometry() {
    }

    public record Bounds(double minX, double minY, double maxX, double maxY) {
        public Bounds {
            minX = finite(minX);
            minY = finite(minY);
            maxX = Math.max(minX, finite(maxX));
            maxY = Math.max(minY, finite(maxY));
        }

        public double width() { return maxX - minX; }
        public double height() { return maxY - minY; }
        public double centerX() { return minX + width() / 2.0; }
        public double centerY() { return minY + height() / 2.0; }
    }

    public record Transform(double translateX, double translateY, double scaleX, double scaleY,
                            double rotationDegrees) {
        public Transform {
            translateX = finite(translateX);
            translateY = finite(translateY);
            scaleX = finite(scaleX) == 0.0 ? 1.0 : scaleX;
            scaleY = finite(scaleY) == 0.0 ? 1.0 : scaleY;
            rotationDegrees = finite(rotationDegrees);
        }

        public static Transform identity() { return new Transform(0, 0, 1, 1, 0); }
    }

    /** Closed contours can be painted by the rendering layer with this polygon and color. */
    public record ClosedContour(List<InkPoint> points, Bounds bounds, double safetyBand) {
        public ClosedContour {
            points = points == null ? List.of() : List.copyOf(points);
            bounds = bounds == null ? new Bounds(0, 0, 0, 0) : bounds;
            safetyBand = Math.max(MIN_SAFETY_BAND, Math.min(MAX_SAFETY_BAND,
                    finite(safetyBand)));
        }
    }

    public static Bounds bounds(InkStroke stroke) {
        if (stroke == null || stroke.points().isEmpty()) return new Bounds(0, 0, 0, 0);
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double radius = stroke.width() / 2.0;
        for (InkPoint point : stroke.points()) {
            if (point == null) continue;
            minX = Math.min(minX, point.x() - radius);
            minY = Math.min(minY, point.y() - radius);
            maxX = Math.max(maxX, point.x() + radius);
            maxY = Math.max(maxY, point.y() + radius);
        }
        return Double.isFinite(minX) ? new Bounds(minX, minY, maxX, maxY) : new Bounds(0, 0, 0, 0);
    }

    /** Returns the deterministic pointer tolerance for this stroke. */
    public static double safetyBand(InkStroke stroke) {
        if (stroke == null) return MIN_SAFETY_BAND;
        double widthBand = Math.max(MIN_SAFETY_BAND, stroke.width() * 1.5);
        double spacing = averagePointSpacing(stroke.points());
        double spacingBand = Double.isFinite(spacing) ? spacing * 0.35 : widthBand;
        return Math.max(MIN_SAFETY_BAND, Math.min(MAX_SAFETY_BAND,
                Math.max(widthBand, spacingBand)));
    }

    public static boolean hitTest(InkStroke stroke, double x, double y) {
        if (stroke == null || stroke.points().isEmpty() || !Double.isFinite(x) || !Double.isFinite(y)) {
            return false;
        }
        double tolerance = safetyBand(stroke);
        List<InkPoint> points = stroke.points();
        if (points.size() == 1) return distance(points.get(0).x(), points.get(0).y(), x, y) <= tolerance;
        for (int i = 1; i < points.size(); i++) {
            InkPoint a = points.get(i - 1);
            InkPoint b = points.get(i);
            if (distanceToSegment(a.x(), a.y(), b.x(), b.y(), x, y) <= tolerance) return true;
        }
        return false;
    }

    public static boolean isClosed(InkStroke stroke) {
        if (stroke == null || stroke.points().size() < MIN_CLOSED_POINTS) return false;
        InkPoint first = stroke.points().get(0);
        InkPoint last = stroke.points().get(stroke.points().size() - 1);
        double closureTolerance = Math.max(8.0, stroke.width() * 2.5);
        return distance(first.x(), first.y(), last.x(), last.y()) <= closureTolerance;
    }

    public static Optional<ClosedContour> closedContour(InkStroke stroke) {
        if (!isClosed(stroke)) return Optional.empty();
        return Optional.of(new ClosedContour(stroke.points(), bounds(stroke), safetyBand(stroke)));
    }

    /**
     * Point-in-contour query for the optional solid-fill tool. The safety band is deliberately not
     * used to expand the fill itself; it only protects hit testing and contour acquisition.
     */
    public static boolean contains(ClosedContour contour, double x, double y) {
        if (contour == null || contour.points().size() < MIN_CLOSED_POINTS
                || !Double.isFinite(x) || !Double.isFinite(y)) return false;
        boolean inside = false;
        List<InkPoint> points = contour.points();
        for (int i = 0, j = points.size() - 1; i < points.size(); j = i++) {
            InkPoint a = points.get(i);
            InkPoint b = points.get(j);
            boolean crosses = (a.y() > y) != (b.y() > y);
            if (crosses) {
                double intersection = (b.x() - a.x()) * (y - a.y())
                        / (b.y() - a.y()) + a.x();
                if (x < intersection) inside = !inside;
            }
        }
        return inside;
    }

    /** Applies translate/scale/rotation around the stroke's own bounds center. */
    public static InkStroke transform(InkStroke stroke, Transform transform) {
        if (stroke == null || stroke.points().isEmpty()) return stroke;
        Transform safe = transform == null ? Transform.identity() : transform;
        Bounds bounds = bounds(stroke);
        double radians = Math.toRadians(safe.rotationDegrees());
        double sin = Math.sin(radians);
        double cos = Math.cos(radians);
        List<InkPoint> transformed = new ArrayList<>(stroke.points().size());
        for (InkPoint point : stroke.points()) {
            double localX = (point.x() - bounds.centerX()) * safe.scaleX();
            double localY = (point.y() - bounds.centerY()) * safe.scaleY();
            double rotatedX = localX * cos - localY * sin;
            double rotatedY = localX * sin + localY * cos;
            transformed.add(InkPoint.of(
                    bounds.centerX() + rotatedX + safe.translateX(),
                    bounds.centerY() + rotatedY + safe.translateY(),
                    point.nanos(), point.pressure()));
        }
        return new InkStroke(stroke.tool(), stroke.color(), stroke.width(), transformed);
    }

    public static List<InkStroke> transformAll(List<InkStroke> strokes, Transform transform) {
        if (strokes == null || strokes.isEmpty()) return List.of();
        return strokes.stream().map(stroke -> transform(stroke, transform)).toList();
    }

    /** Same affine transform for a fill seed or a stroke point about a shared group pivot. */
    public static InkPoint transformPoint(InkPoint point, double centerX, double centerY, Transform transform) {
        double x=(point.x()-centerX)*transform.scaleX();
        double y=(point.y()-centerY)*transform.scaleY();
        double radians=Math.toRadians(transform.rotationDegrees());
        double c=Math.cos(radians), s=Math.sin(radians);
        return InkPoint.of(centerX+x*c-y*s+transform.translateX(),
                centerY+x*s+y*c+transform.translateY(),point.nanos(),point.pressure());
    }

    private static double averagePointSpacing(List<InkPoint> points) {
        if (points == null || points.size() < 2) return Double.NaN;
        double total = 0.0;
        int count = 0;
        for (int i = 1; i < points.size(); i++) {
            InkPoint a = points.get(i - 1);
            InkPoint b = points.get(i);
            total += distance(a.x(), a.y(), b.x(), b.y());
            count++;
        }
        return count == 0 ? Double.NaN : total / count;
    }

    private static double distanceToSegment(double ax, double ay, double bx, double by, double px, double py) {
        double dx = bx - ax;
        double dy = by - ay;
        if (dx == 0.0 && dy == 0.0) return distance(ax, ay, px, py);
        double projection = ((px - ax) * dx + (py - ay) * dy) / (dx * dx + dy * dy);
        projection = Math.max(0.0, Math.min(1.0, projection));
        return distance(ax + projection * dx, ay + projection * dy, px, py);
    }

    private static double distance(double ax, double ay, double bx, double by) {
        return Math.hypot(ax - bx, ay - by);
    }

    private static double finite(double value) {
        return Double.isFinite(value) ? value : 0.0;
    }
}

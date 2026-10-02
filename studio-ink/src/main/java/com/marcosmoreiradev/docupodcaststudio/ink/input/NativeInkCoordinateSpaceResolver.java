package com.marcosmoreiradev.docupodcaststudio.ink.input;

import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.transform.Transform;
import javafx.stage.Window;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Resolves native stylus coordinates to the real JavaFX input target.
 *
 * <p>LectureStudio native packets can arrive in different coordinate spaces
 * depending on the Windows hook. This resolver locks one space per target while
 * drawing, instead of switching candidates mid-stroke.</p>
 */
public final class NativeInkCoordinateSpaceResolver {
    public static final String COORDINATE_SPACE_PROPERTY = "docupodcast.ink.lectureStudioCoordinateSpace";
    private static final long ANCHOR_MAX_AGE_NANOS = 1_500_000_000L;
    private static final double ANCHOR_MATCH_TOLERANCE = 24.0;

    private CoordinateSpace lockedSpace;
    private GeometrySnapshot geometrySnapshot;
    private Point2D lastJavaFxAnchorTargetLocal;
    private GeometrySnapshot lastJavaFxAnchorGeometry;
    private long lastJavaFxAnchorNanos;

    public void reset() {
        lockedSpace = null;
        geometrySnapshot = null;
        lastJavaFxAnchorTargetLocal = null;
        lastJavaFxAnchorGeometry = null;
        lastJavaFxAnchorNanos = 0L;
    }

    public void clearLockedSpace() {
        lockedSpace = null;
        geometrySnapshot = null;
    }

    public void recordJavaFxAnchor(Node target, double sceneX, double sceneY) {
        if (target == null || !Double.isFinite(sceneX) || !Double.isFinite(sceneY)) {
            return;
        }
        Point2D targetLocal = target.sceneToLocal(sceneX, sceneY);
        if (targetLocal == null
                || !Double.isFinite(targetLocal.getX())
                || !Double.isFinite(targetLocal.getY())
                || !insideTargetBounds(target, targetLocal)) {
            return;
        }
        lastJavaFxAnchorTargetLocal = targetLocal;
        lastJavaFxAnchorGeometry = GeometrySnapshot.capture(target);
        lastJavaFxAnchorNanos = System.nanoTime();
    }

    public TranslationResult translate(Node target, double rawX, double rawY, boolean strokeInProgress) {
        return translate(target, rawX, rawY, strokeInProgress, InkInputCursor.UNKNOWN);
    }

    public TranslationResult translate(Node target,
                                       double rawX,
                                       double rawY,
                                       boolean strokeInProgress,
                                       InkInputCursor cursor) {
        if (target == null || !Double.isFinite(rawX) || !Double.isFinite(rawY)) {
            return TranslationResult.rejected("target/raw invalido");
        }
        GeometrySnapshot currentGeometry = GeometrySnapshot.capture(target);
        if (!strokeInProgress && !currentGeometry.equals(geometrySnapshot)) {
            lockedSpace = null;
            geometrySnapshot = currentGeometry;
        } else if (geometrySnapshot == null) {
            geometrySnapshot = currentGeometry;
        }

        List<Candidate> candidates = candidates(target, rawX, rawY);
        List<Candidate> inside = candidates.stream()
                .filter(candidate -> insideTargetBounds(target, candidate.point()))
                .toList();
        if (inside.isEmpty()) {
            return TranslationResult.rejected("raw fuera del target para todos los espacios");
        }

        CoordinateSpace forcedSpace = forcedCoordinateSpace().orElse(null);
        if (forcedSpace != null) {
            return inside.stream()
                    .filter(candidate -> candidate.space() == forcedSpace)
                    .findFirst()
                    .map(candidate -> accept(candidate, false))
                    .orElseGet(() -> TranslationResult.rejected(
                            "espacio forzado " + forcedSpace.diagnosticLabel() + " fuera del target"));
        }

        if (strokeInProgress && lockedSpace != null) {
            return inside.stream()
                    .filter(candidate -> candidate.space() == lockedSpace)
                    .findFirst()
                    .map(candidate -> accept(candidate, false))
                    .orElseGet(() -> TranslationResult.rejected(
                            "espacio bloqueado " + lockedSpace.diagnosticLabel() + " fuera del target"));
        }

        Optional<Candidate> selected = selectCandidate(inside, currentGeometry, cursor);
        if (selected.isEmpty()) {
            return TranslationResult.rejected("sin espacio de coordenadas automatico confiable");
        }
        lockedSpace = selected.get().space();
        return accept(selected.get(), true);
    }

    public String lockedSpaceLabel() {
        return lockedSpace == null ? "sin-coords" : lockedSpace.diagnosticLabel();
    }

    private Optional<Candidate> selectCandidate(List<Candidate> inside, GeometrySnapshot currentGeometry, InkInputCursor cursor) {
        if (lockedSpace != null) {
            Optional<Candidate> locked = inside.stream()
                    .filter(candidate -> candidate.space() == lockedSpace)
                    .findFirst();
            if (locked.isPresent()) {
                return locked;
            }
        }
        Optional<Candidate> anchored = selectByRecentJavaFxAnchor(inside, currentGeometry);
        if (anchored.isPresent()) {
            return anchored;
        }
        if (hasMultipleDistinctPoints(inside)) {
            return Optional.empty();
        }
        for (CoordinateSpace preferred : CoordinateSpace.preferredAutoOrder(cursor)) {
            Optional<Candidate> selected = inside.stream()
                    .filter(candidate -> candidate.space() == preferred)
                    .findFirst();
            if (selected.isPresent()) {
                return selected;
            }
        }
        return Optional.empty();
    }

    private static boolean hasMultipleDistinctPoints(List<Candidate> candidates) {
        Point2D first = null;
        for (Candidate candidate : candidates) {
            if (candidate == null || candidate.point() == null) {
                continue;
            }
            if (first == null) {
                first = candidate.point();
                continue;
            }
            if (first.distance(candidate.point()) > 1.0) {
                return true;
            }
        }
        return false;
    }

    private Optional<Candidate> selectByRecentJavaFxAnchor(List<Candidate> inside,
                                                           GeometrySnapshot currentGeometry) {
        if (lastJavaFxAnchorTargetLocal == null
                || lastJavaFxAnchorGeometry == null
                || currentGeometry == null
                || !lastJavaFxAnchorGeometry.equals(currentGeometry)
                || System.nanoTime() - lastJavaFxAnchorNanos > ANCHOR_MAX_AGE_NANOS) {
            return Optional.empty();
        }
        Candidate best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Candidate candidate : inside) {
            double distance = candidate.point().distance(lastJavaFxAnchorTargetLocal);
            if (distance < bestDistance
                    || (Math.abs(distance - bestDistance) < 0.001
                    && anchorTieBreakRank(candidate.space()) < anchorTieBreakRank(best == null ? null : best.space()))) {
                best = candidate;
                bestDistance = distance;
            }
        }
        return best != null && bestDistance <= ANCHOR_MATCH_TOLERANCE
                ? Optional.of(best)
                : Optional.empty();
    }

    private static int anchorTieBreakRank(CoordinateSpace space) {
        if (space == null) {
            return Integer.MAX_VALUE;
        }
        return switch (space) {
            case WINDOW_CLIENT_LOGICAL -> 0;
            case SCREEN_LOGICAL -> 1;
            case TARGET_LOCAL -> 2;
            case WINDOW_CLIENT_PHYSICAL -> 3;
            case SCREEN_PHYSICAL -> 4;
        };
    }

    private TranslationResult accept(Candidate candidate, boolean lockedNow) {
        return new TranslationResult(
                true,
                candidate.point(),
                candidate.space(),
                candidate.space().diagnosticLabel(),
                "",
                lockedNow);
    }

    private static List<Candidate> candidates(Node target, double rawX, double rawY) {
        double scaleX = outputScaleX(target);
        double scaleY = outputScaleY(target);
        List<Candidate> candidates = new ArrayList<>();
        addCandidate(candidates, CoordinateSpace.WINDOW_CLIENT_PHYSICAL,
                target.sceneToLocal(rawX / scaleX, rawY / scaleY));
        addCandidate(candidates, CoordinateSpace.WINDOW_CLIENT_LOGICAL,
                target.sceneToLocal(rawX, rawY));
        Point2D screenPhysical = target.screenToLocal(rawX / scaleX, rawY / scaleY);
        addCandidate(candidates, CoordinateSpace.SCREEN_PHYSICAL, screenPhysical);
        Point2D screenLogical = target.screenToLocal(rawX, rawY);
        addCandidate(candidates, CoordinateSpace.SCREEN_LOGICAL, screenLogical);
        addCandidate(candidates, CoordinateSpace.TARGET_LOCAL, new Point2D(rawX, rawY));
        return candidates;
    }

    private static void addCandidate(List<Candidate> candidates, CoordinateSpace space, Point2D point) {
        if (point != null && Double.isFinite(point.getX()) && Double.isFinite(point.getY())) {
            candidates.add(new Candidate(space, point));
        }
    }

    private static boolean insideTargetBounds(Node target, Point2D point) {
        if (target == null || point == null || !Double.isFinite(point.getX()) || !Double.isFinite(point.getY())) {
            return false;
        }
        Bounds bounds = target.getLayoutBounds();
        return bounds != null
                && point.getX() >= bounds.getMinX()
                && point.getY() >= bounds.getMinY()
                && point.getX() <= bounds.getMaxX()
                && point.getY() <= bounds.getMaxY();
    }

    private static double outputScaleX(Node target) {
        Window window = target == null || target.getScene() == null ? null : target.getScene().getWindow();
        double scale = window == null ? 1.0 : window.getOutputScaleX();
        return scale > 0.0 && Double.isFinite(scale) ? scale : 1.0;
    }

    private static double outputScaleY(Node target) {
        Window window = target == null || target.getScene() == null ? null : target.getScene().getWindow();
        double scale = window == null ? 1.0 : window.getOutputScaleY();
        return scale > 0.0 && Double.isFinite(scale) ? scale : 1.0;
    }

    private static Optional<CoordinateSpace> forcedCoordinateSpace() {
        String value = System.getProperty(COORDINATE_SPACE_PROPERTY, "").strip();
        if (value.isBlank() || "auto".equalsIgnoreCase(value)) {
            return Optional.empty();
        }
        for (CoordinateSpace space : CoordinateSpace.values()) {
            if (space.propertyName().equalsIgnoreCase(value) || space.name().equalsIgnoreCase(value)) {
                return Optional.of(space);
            }
        }
        return Optional.empty();
    }

    public enum CoordinateSpace {
        WINDOW_CLIENT_PHYSICAL("cliente-fisico", "window-client-physical"),
        WINDOW_CLIENT_LOGICAL("cliente-logico", "window-client-logical"),
        SCREEN_PHYSICAL("pantalla-fisica", "screen-physical"),
        SCREEN_LOGICAL("pantalla-logica", "screen-logical"),
        TARGET_LOCAL("target-local", "target-local");

        private static final CoordinateSpace[] PREFERRED_AUTO_ORDER = {
                WINDOW_CLIENT_LOGICAL,
                WINDOW_CLIENT_PHYSICAL,
                SCREEN_LOGICAL,
                SCREEN_PHYSICAL
        };
        private static final CoordinateSpace[] MOUSE_AUTO_ORDER = {
                WINDOW_CLIENT_LOGICAL,
                WINDOW_CLIENT_PHYSICAL,
                SCREEN_LOGICAL,
                SCREEN_PHYSICAL
        };

        private final String diagnosticLabel;
        private final String propertyName;

        CoordinateSpace(String diagnosticLabel, String propertyName) {
            this.diagnosticLabel = diagnosticLabel;
            this.propertyName = propertyName;
        }

        public String diagnosticLabel() {
            return diagnosticLabel;
        }

        public String propertyName() {
            return propertyName;
        }

        static CoordinateSpace[] preferredAutoOrder(InkInputCursor cursor) {
            return cursor == InkInputCursor.MOUSE ? MOUSE_AUTO_ORDER : PREFERRED_AUTO_ORDER;
        }
    }

    public record TranslationResult(
            boolean accepted,
            Point2D point,
            CoordinateSpace space,
            String diagnosticLabel,
            String rejectionReason,
            boolean lockedNow) {
        static TranslationResult rejected(String reason) {
            return new TranslationResult(false, null, null, "", reason == null ? "" : reason, false);
        }
    }

    private record Candidate(CoordinateSpace space, Point2D point) {
    }

    private record GeometrySnapshot(
            double minX,
            double minY,
            double width,
            double height,
            double mxx,
            double mxy,
            double tx,
            double myx,
            double myy,
            double ty,
            double scaleX,
            double scaleY) {
        static GeometrySnapshot capture(Node target) {
            Bounds bounds = target.getLayoutBounds();
            Transform transform = target.getLocalToSceneTransform();
            return new GeometrySnapshot(
                    round(bounds == null ? 0.0 : bounds.getMinX()),
                    round(bounds == null ? 0.0 : bounds.getMinY()),
                    round(bounds == null ? 0.0 : bounds.getWidth()),
                    round(bounds == null ? 0.0 : bounds.getHeight()),
                    round(transform == null ? 1.0 : transform.getMxx()),
                    round(transform == null ? 0.0 : transform.getMxy()),
                    round(transform == null ? 0.0 : transform.getTx()),
                    round(transform == null ? 0.0 : transform.getMyx()),
                    round(transform == null ? 1.0 : transform.getMyy()),
                    round(transform == null ? 0.0 : transform.getTy()),
                    round(outputScaleX(target)),
                    round(outputScaleY(target)));
        }

        private static double round(double value) {
            if (!Double.isFinite(value)) {
                return 0.0;
            }
            return Double.parseDouble(String.format(Locale.ROOT, "%.3f", value));
        }
    }
}

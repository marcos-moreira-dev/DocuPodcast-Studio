package com.marcosmoreiradev.docupodcaststudio.ink.canvas;

import com.marcosmoreiradev.docupodcaststudio.ink.DrawingProfile;
import com.marcosmoreiradev.docupodcaststudio.ink.ViewportMode;
import com.marcosmoreiradev.docupodcaststudio.ink.input.InkInputSample;
import javafx.geometry.Point2D;
import javafx.scene.Node;

import java.util.Objects;
import java.util.Optional;

/**
 * Shared live coordinate space for fixed and growing ink canvases.
 * DrawingProfile dimensions are minima for growing viewports, never clamps.
 */
public final class InkCanvasViewport {
    private final InkCanvasSurface surface;
    private final DrawingProfile profile;

    public InkCanvasViewport(InkCanvasSurface surface, DrawingProfile profile) {
        this.surface = Objects.requireNonNull(surface, "ink canvas surface");
        this.profile = Objects.requireNonNull(profile, "drawing profile");
    }

    public InkCanvasSurface surface() { return surface; }
    public DrawingProfile profile() { return profile; }
    public Node inputTarget() { return surface.inkInputTarget(); }
    public double logicalWidth() { return surface.logicalWidth(); }
    public double logicalHeight() { return surface.logicalHeight(); }

    public Optional<InkInputSample> mapInside(InkInputSample sample) {
        if (sample == null) return Optional.empty();
        Optional<Point2D> mapped = InkCanvasViewportCoordinateMapper.mapInside(
                inputTarget(), surface, sample.x(), sample.y(), logicalWidth(), logicalHeight());
        return mapped.map(point -> withPoint(sample, point));
    }

    public boolean ensureCoverage(double viewportWidth, double viewportHeight, double zoom) {
        if (profile.viewportMode() != ViewportMode.GROWING) return false;
        double safeZoom = Double.isFinite(zoom) ? Math.max(0.1, zoom) : 1.0;
        double beforeWidth = surface.logicalWidth();
        double beforeHeight = surface.logicalHeight();
        surface.ensureLogicalSize(
                Math.max(profile.logicalWidth(), Math.max(1.0, viewportWidth) / safeZoom),
                Math.max(profile.logicalHeight(), Math.max(1.0, viewportHeight) / safeZoom));
        return surface.logicalWidth() != beforeWidth || surface.logicalHeight() != beforeHeight;
    }

    public boolean growNear(InkInputSample sample) {
        if (sample == null || profile.viewportMode() != ViewportMode.GROWING) return false;
        double beforeWidth = surface.logicalWidth();
        double beforeHeight = surface.logicalHeight();
        surface.growForPoint(sample.x(), sample.y(), TiledInkCanvasSurface.EDGE_GROW_THRESHOLD);
        return surface.logicalWidth() != beforeWidth || surface.logicalHeight() != beforeHeight;
    }

    private static InkInputSample withPoint(InkInputSample sample, Point2D point) {
        return new InkInputSample(point.getX(), point.getY(), sample.nanos(), sample.pressure(), sample.cursor(),
                sample.primaryButtonDown(), sample.eraserButton(), sample.rawPressure(), sample.inputSource());
    }
}

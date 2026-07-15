package com.marcosmoreiradev.docupodcaststudio.presentation.ink;

import java.util.ArrayList;
import java.util.List;

/** Shared ink sampling helpers for live renderers. */
public final class InkStrokePipeline {
    private InkStrokePipeline() {
    }

    public static List<InkPipelinePoint> resampleLine(double fromX, double fromY,
                                                      double toX, double toY,
                                                      double maxSpacing) {
        double spacing = Math.max(0.5, Double.isFinite(maxSpacing) ? maxSpacing : 2.5);
        double distance = Math.hypot(toX - fromX, toY - fromY);
        int steps = Math.max(1, (int) Math.ceil(distance / spacing));
        List<InkPipelinePoint> points = new ArrayList<>(steps);
        for (int step = 1; step <= steps; step++) {
            double t = (double) step / steps;
            points.add(new InkPipelinePoint(
                    fromX + (toX - fromX) * t,
                    fromY + (toY - fromY) * t));
        }
        return points;
    }

    public record InkPipelinePoint(double x, double y) {
        public InkPipelinePoint {
            x = Double.isFinite(x) ? x : 0.0;
            y = Double.isFinite(y) ? y : 0.0;
        }
    }
}

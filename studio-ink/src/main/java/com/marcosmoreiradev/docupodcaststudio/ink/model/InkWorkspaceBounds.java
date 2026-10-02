package com.marcosmoreiradev.docupodcaststudio.ink.model;

import java.util.Collection;

/** Utility bounds for exports and workspace diagnostics. */
public record InkWorkspaceBounds(double minX, double minY, double maxX, double maxY) {
    public InkWorkspaceBounds {
        minX = finiteOrZero(minX);
        minY = finiteOrZero(minY);
        maxX = Math.max(minX, finiteOrZero(maxX));
        maxY = Math.max(minY, finiteOrZero(maxY));
    }

    public static InkWorkspaceBounds empty() {
        return new InkWorkspaceBounds(0, 0, 0, 0);
    }

    public static InkWorkspaceBounds from(Collection<InkStroke> strokes, Collection<InkPlacedImage> images) {
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = 0;
        double maxY = 0;
        if (images != null) {
            for (InkPlacedImage image : images) {
                if (image == null) {
                    continue;
                }
                minX = Math.min(minX, image.x());
                minY = Math.min(minY, image.y());
                maxX = Math.max(maxX, image.maxX());
                maxY = Math.max(maxY, image.maxY());
            }
        }
        if (strokes != null) {
            for (InkStroke stroke : strokes) {
                if (stroke == null) {
                    continue;
                }
                double pad = Math.max(1.0, stroke.width()) + 2.0;
                for (InkPoint point : stroke.points()) {
                    minX = Math.min(minX, point.x() - pad);
                    minY = Math.min(minY, point.y() - pad);
                    maxX = Math.max(maxX, point.x() + pad);
                    maxY = Math.max(maxY, point.y() + pad);
                }
            }
        }
        if (!Double.isFinite(minX) || !Double.isFinite(minY)) {
            return empty();
        }
        return new InkWorkspaceBounds(Math.max(0, minX), Math.max(0, minY), maxX, maxY);
    }

    public InkWorkspaceBounds expandedBy(double amount) {
        double safe = Math.max(0.0, Double.isFinite(amount) ? amount : 0.0);
        return new InkWorkspaceBounds(
                Math.max(0, minX - safe),
                Math.max(0, minY - safe),
                maxX + safe,
                maxY + safe);
    }

    private static double finiteOrZero(double value) {
        return Double.isFinite(value) ? value : 0.0;
    }
}

package com.marcosmoreiradev.docupodcaststudio.ink.model;

import java.util.List;
import java.util.Map;

/** Reusable sidecar state for technical problems now and theatre sketches later. */
public record InkWorkspaceState(
        int version,
        double logicalWidth,
        double logicalHeight,
        String background,
        List<InkStroke> strokes,
        List<InkPlacedImage> images,
        Map<String, String> metadata) {

    public static final int CURRENT_VERSION = 3;

    public InkWorkspaceState {
        version = version <= 0 ? CURRENT_VERSION : version;
        logicalWidth = Math.max(1.0, Double.isFinite(logicalWidth) ? logicalWidth : 1.0);
        logicalHeight = Math.max(1.0, Double.isFinite(logicalHeight) ? logicalHeight : 1.0);
        background = background == null || background.isBlank() ? "#ffffffff" : background;
        strokes = strokes == null ? List.of() : List.copyOf(strokes);
        images = images == null ? List.of() : List.copyOf(images);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public static InkWorkspaceState create(double logicalWidth, double logicalHeight, String background,
                                           List<InkStroke> strokes, List<InkPlacedImage> images,
                                           Map<String, String> metadata) {
        return new InkWorkspaceState(CURRENT_VERSION, logicalWidth, logicalHeight, background,
                strokes, images, metadata);
    }

    public InkWorkspaceBounds contentBounds() {
        return InkWorkspaceBounds.from(strokes, images);
    }
}

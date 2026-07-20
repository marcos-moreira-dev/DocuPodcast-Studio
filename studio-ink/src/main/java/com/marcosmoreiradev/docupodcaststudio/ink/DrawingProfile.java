package com.marcosmoreiradev.docupodcaststudio.ink;

import java.util.List;
import java.util.Objects;

/** Declarative composition profile shared by every ink editor. */
public record DrawingProfile(
        String id,
        String displayName,
        ViewportMode viewportMode,
        double logicalWidth,
        double logicalHeight,
        InkInputPolicy inputPolicy,
        List<DrawingToolId> tools,
        int historyLimit,
        boolean zoomEnabled,
        DrawingExportProfile exportProfile) {
    public DrawingProfile {
        id = require(id, "drawing profile id");
        displayName = require(displayName, "drawing profile name");
        viewportMode = Objects.requireNonNullElse(viewportMode, ViewportMode.FIXED);
        logicalWidth = Math.max(1.0, logicalWidth);
        logicalHeight = Math.max(1.0, logicalHeight);
        inputPolicy = Objects.requireNonNullElse(inputPolicy, InkInputPolicy.MOUSE_AND_NATIVE);
        tools = tools == null ? List.of() : List.copyOf(tools);
        historyLimit = Math.max(1, Math.min(500, historyLimit));
        exportProfile = Objects.requireNonNullElse(exportProfile, new DrawingExportProfile(1, false, true));
    }

    public boolean supports(DrawingToolId tool) { return tool != null && tools.contains(tool); }

    private static String require(String value, String field) {
        String text = value == null ? "" : value.strip();
        if (text.isBlank()) throw new IllegalArgumentException(field + " is required");
        return text;
    }
}

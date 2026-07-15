package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import javafx.scene.image.WritableImage;

import java.util.List;

/** Composed PNG-ready canvas image plus export diagnostics. */
public record StudyProblemCanvasExportResult(
        WritableImage image,
        int scale,
        boolean cropped,
        int logicalWidth,
        int logicalHeight,
        List<String> warnings
) {
    public StudyProblemCanvasExportResult {
        scale = Math.max(1, scale);
        logicalWidth = Math.max(1, logicalWidth);
        logicalHeight = Math.max(1, logicalHeight);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }
}

package com.marcosmoreiradev.docupodcaststudio.application.document;

import java.nio.file.Path;
import java.util.List;

/** PNG crop produced from a PDF visual region selection. */
public record PdfRegionCaptureResult(
        int sourcePage,
        String bbox,
        int dpi,
        int widthPixels,
        int heightPixels,
        Path pngPath,
        List<String> warnings
) {
    public PdfRegionCaptureResult {
        sourcePage = Math.max(0, sourcePage);
        bbox = bbox == null ? "" : bbox.strip();
        dpi = Math.max(0, dpi);
        widthPixels = Math.max(0, widthPixels);
        heightPixels = Math.max(0, heightPixels);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }
}

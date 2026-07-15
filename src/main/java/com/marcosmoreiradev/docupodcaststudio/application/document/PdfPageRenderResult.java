package com.marcosmoreiradev.docupodcaststudio.application.document;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Objects;

/** Rendered PDF page or crop plus source page metadata. */
public record PdfPageRenderResult(
        int pageNumber,
        int pageCount,
        double pageWidthPoints,
        double pageHeightPoints,
        int dpi,
        BufferedImage image,
        String renderMode,
        List<String> warnings
) {
    public PdfPageRenderResult {
        if (pageNumber <= 0) {
            throw new IllegalArgumentException("pageNumber must be positive");
        }
        if (pageCount < pageNumber) {
            pageCount = pageNumber;
        }
        image = Objects.requireNonNull(image, "image");
        renderMode = renderMode == null ? "" : renderMode.strip();
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }
}

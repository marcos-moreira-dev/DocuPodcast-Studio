package com.marcosmoreiradev.docupodcaststudio.application.document;

import java.nio.file.Path;
import java.util.List;

/** Future viewer descriptor for a visually renderable PDF source. */
public record PdfVisualDocument(Path sourcePath, int pageCount, List<PdfVisualPage> pages, List<String> warnings) {
    public PdfVisualDocument {
        pageCount = Math.max(0, pageCount);
        pages = pages == null ? List.of() : List.copyOf(pages);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }
}

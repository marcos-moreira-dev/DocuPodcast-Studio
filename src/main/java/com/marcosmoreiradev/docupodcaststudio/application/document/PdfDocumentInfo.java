package com.marcosmoreiradev.docupodcaststudio.application.document;

import java.nio.file.Path;
import java.util.List;

/** Renderer-neutral metadata for a PDF source. */
public record PdfDocumentInfo(
        Path sourcePath,
        int pageCount,
        boolean encrypted,
        boolean visuallyRenderable,
        String title,
        List<PdfPageInfo> pages,
        List<String> warnings
) {
    public PdfDocumentInfo {
        if (pageCount < 0) {
            pageCount = 0;
        }
        title = title == null ? "" : title.strip();
        pages = pages == null ? List.of() : List.copyOf(pages);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }

    public static PdfDocumentInfo unavailable(Path sourcePath, int pageCount, String warning) {
        return new PdfDocumentInfo(sourcePath, Math.max(0, pageCount), false, false, "",
                List.of(), warning == null || warning.isBlank() ? List.of() : List.of(warning));
    }
}

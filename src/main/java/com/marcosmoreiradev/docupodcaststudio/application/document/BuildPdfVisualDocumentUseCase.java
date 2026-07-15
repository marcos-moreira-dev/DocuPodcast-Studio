package com.marcosmoreiradev.docupodcaststudio.application.document;

import java.nio.file.Path;
import java.util.Objects;

/** Builds a renderer-neutral PDF visual model for the future PDF workspace. */
public final class BuildPdfVisualDocumentUseCase {
    private final PdfRenderEngine renderEngine;

    public BuildPdfVisualDocumentUseCase(PdfRenderEngine renderEngine) {
        this.renderEngine = Objects.requireNonNull(renderEngine, "renderEngine");
    }

    public PdfVisualDocument build(Path sourcePdf, int recommendedDpi) throws PdfRenderException {
        PdfDocumentInfo info = renderEngine.inspect(sourcePdf, PdfOpenOptions.empty());
        return new PdfVisualDocument(
                sourcePdf,
                info.pageCount(),
                info.pages().stream()
                        .map(page -> new PdfVisualPage(page.pageNumber(), page.widthPoints(), page.heightPoints(), recommendedDpi))
                        .toList(),
                info.warnings());
    }
}

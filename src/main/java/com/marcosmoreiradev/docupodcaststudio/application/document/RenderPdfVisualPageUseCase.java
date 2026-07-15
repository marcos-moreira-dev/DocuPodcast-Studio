package com.marcosmoreiradev.docupodcaststudio.application.document;

import java.util.Objects;

/** Renders one PDF page through the application boundary used by visual workspaces. */
public final class RenderPdfVisualPageUseCase {
    private final PdfRenderEngine renderEngine;

    public RenderPdfVisualPageUseCase(PdfRenderEngine renderEngine) {
        this.renderEngine = Objects.requireNonNull(renderEngine, "renderEngine");
    }

    public PdfPageRenderResult render(PdfPageRenderRequest request) throws PdfRenderException {
        return renderEngine.renderPage(Objects.requireNonNull(request, "request"));
    }
}

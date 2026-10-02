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
                        .map(page -> {
                            int rotation = PdfPageCoordinateTransform.normalizeRotation(page.rotation());
                            boolean swapsAxes = rotation == 90 || rotation == 270;
                            return new PdfVisualPage(
                                    page.pageNumber(),
                                    swapsAxes ? page.heightPoints() : page.widthPoints(),
                                    swapsAxes ? page.widthPoints() : page.heightPoints(),
                                    rotation,
                                    recommendedDpi);
                        })
                        .toList(),
                info.warnings());
    }
}

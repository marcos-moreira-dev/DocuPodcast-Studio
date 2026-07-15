package com.marcosmoreiradev.docupodcaststudio.application.document;

import java.nio.file.Path;

/** Transversal embedded PDF render contract, independent from JavaFX presentation. */
public interface PdfRenderEngine {
    PdfDocumentInfo inspect(Path sourcePdf, PdfOpenOptions options) throws PdfRenderException;

    PdfPageRenderResult renderPage(PdfPageRenderRequest request) throws PdfRenderException;

    PdfPageRenderResult renderCrop(PdfCropRenderRequest request) throws PdfRenderException;
}

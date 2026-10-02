package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import com.marcosmoreiradev.docupodcaststudio.application.document.PdfPreparationScope;
import com.marcosmoreiradev.docupodcaststudio.application.document.PreparedPdfSource;

import java.util.function.Consumer;

/** Bridges the PDF index with the shared preparation scheduler without embedding OCR in the view. */
@FunctionalInterface
interface PdfIndexPreparationHandler {
    void prepare(PreparedPdfSource source,
                 PdfPreparationScope scope,
                 int rangeStart,
                 int rangeEnd,
                 Runnable onCompleted,
                 Consumer<String> onStatus);
}

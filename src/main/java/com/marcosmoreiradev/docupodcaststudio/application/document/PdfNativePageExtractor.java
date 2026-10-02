package com.marcosmoreiradev.docupodcaststudio.application.document;

import java.nio.file.Path;

/** Extracts native text and geometry for one page without OCR. */
@FunctionalInterface
public interface PdfNativePageExtractor {
    PdfTextLayer extract(Path sourcePdf, int pageNumber);
}

package com.marcosmoreiradev.docupodcaststudio.application.document;

/** Local OCR contract for PDF pages; presentation must not depend on the concrete backend. */
public interface PdfOcrEngine {
    PdfOcrPageResult recognize(PdfOcrRequest request) throws PdfOcrException;
}

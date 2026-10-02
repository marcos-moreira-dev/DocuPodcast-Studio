package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfDocumentManifest;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PreparedPdfPage;

import java.util.List;

public record PdfLayoutAnalysisResult(
        PdfDocumentManifest manifest,
        List<PreparedPdfPage> pages,
        List<Integer> changedPages
) {
    public PdfLayoutAnalysisResult {
        pages = pages == null ? List.of() : List.copyOf(pages);
        changedPages = changedPages == null ? List.of() : List.copyOf(changedPages);
    }
}

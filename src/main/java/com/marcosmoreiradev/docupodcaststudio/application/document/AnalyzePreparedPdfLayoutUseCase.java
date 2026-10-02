package com.marcosmoreiradev.docupodcaststudio.application.document;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

/** Atomically publishes cross-page layout decisions to manifest and changed pages. */
public final class AnalyzePreparedPdfLayoutUseCase {
    private final PreparedPdfDocumentRepository repository;
    private final PdfDocumentLayoutAnalyzer analyzer;

    public AnalyzePreparedPdfLayoutUseCase(PreparedPdfDocumentRepository repository,
                                           PdfDocumentLayoutAnalyzer analyzer) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.analyzer = analyzer == null ? new PdfDocumentLayoutAnalyzer() : analyzer;
    }

    public PdfLayoutAnalysisResult execute(Path projectRoot) throws IOException {
        var manifest = repository.loadManifest(projectRoot)
                .orElseThrow(() -> new IOException("El proyecto no tiene manifest PDF V2."));
        PdfLayoutAnalysisResult result = analyzer.analyze(manifest, repository.loadPages(projectRoot));
        for (int pageNumber : result.changedPages()) {
            repository.savePage(projectRoot, result.pages().stream()
                    .filter(page -> page.pageNumber() == pageNumber).findFirst().orElseThrow());
        }
        repository.saveManifest(projectRoot, result.manifest());
        return result;
    }
}

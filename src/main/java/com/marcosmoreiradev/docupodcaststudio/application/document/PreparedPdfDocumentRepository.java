package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfDocumentManifest;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PreparedPdfPage;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Collection;

/** Canonical page-oriented persistence port for PDF workspaces. */
public interface PreparedPdfDocumentRepository {
    void initialize(Path projectRoot, PdfDocumentManifest manifest) throws IOException;

    Optional<PdfDocumentManifest> loadManifest(Path projectRoot) throws IOException;

    void saveManifest(Path projectRoot, PdfDocumentManifest manifest) throws IOException;

    Optional<PreparedPdfPage> loadPage(Path projectRoot, int pageNumber) throws IOException;

    List<PreparedPdfPage> loadPages(Path projectRoot) throws IOException;

    default List<Integer> listPreparedPageNumbers(Path projectRoot) throws IOException {
        return loadPages(projectRoot).stream()
                .map(PreparedPdfPage::pageNumber)
                .sorted()
                .toList();
    }

    default List<PreparedPdfPage> loadPages(Path projectRoot,
                                            Collection<Integer> pageNumbers) throws IOException {
        if (pageNumbers == null || pageNumbers.isEmpty()) return List.of();
        java.util.HashSet<Integer> requested = new java.util.HashSet<>(pageNumbers);
        return loadPages(projectRoot).stream()
                .filter(page -> requested.contains(page.pageNumber()))
                .toList();
    }

    default PreparedPdfRegionIndex loadRegionIndex(Path projectRoot) throws IOException {
        return PreparedPdfRegionIndex.from(loadPages(projectRoot));
    }

    void savePage(Path projectRoot, PreparedPdfPage page) throws IOException;
}

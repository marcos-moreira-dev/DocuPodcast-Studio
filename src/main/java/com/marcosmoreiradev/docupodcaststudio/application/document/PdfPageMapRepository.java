package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfPageMap;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfPageMapManifest;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

/** Persistence port for the independently removable PdfPageMap sidecar. */
public interface PdfPageMapRepository {
    Optional<PdfPageMapManifest> loadManifest(Path projectRoot) throws IOException;

    Optional<PdfPageMap> loadPage(Path projectRoot, int pageNumber) throws IOException;

    void savePage(Path projectRoot, String sourceSha256, PdfPageMap pageMap) throws IOException;
}

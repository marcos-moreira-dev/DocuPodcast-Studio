package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfDocumentManifest;

import java.nio.file.Path;
import java.time.Instant;

final class PreparedPdfTestFixtures {
    static final String SHA = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    private PreparedPdfTestFixtures() {
    }

    static PreparedPdfWorkspaceRef workspace(InMemoryPreparedPdfDocumentRepository repository,
                                             Path root,
                                             int pages) {
        Path normalized = root.toAbsolutePath().normalize();
        repository.initialize(normalized, new PdfDocumentManifest(
                PdfDocumentManifest.CURRENT_SCHEMA_VERSION, "PDF", "sample.pdf", SHA,
                pages, "test", Instant.EPOCH, Instant.EPOCH));
        return new PreparedPdfWorkspaceRef(normalized, normalized.resolve("source/sample.pdf"), SHA);
    }
}

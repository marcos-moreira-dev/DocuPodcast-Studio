package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfDocumentManifest;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfReadingStrategy;
import java.io.IOException;
import java.util.Objects;

/** Read and change a document preference without preparing pages or invalidating audio. */
public final class PdfReadingPreferencesUseCase {
    private final PreparedPdfDocumentRepository repository;
    public PdfReadingPreferencesUseCase(PreparedPdfDocumentRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }
    public PdfDocumentManifest read(PreparedPdfWorkspaceRef workspace) throws IOException {
        return repository.loadManifest(workspace.projectRoot())
                .orElseThrow(() -> new IOException("No se encontró la configuración del PDF."));
    }
    public void update(PreparedPdfWorkspaceRef workspace, PdfReadingStrategy strategy) throws IOException {
        PdfDocumentManifest manifest = read(workspace);
        if (manifest.readingStrategy() != Objects.requireNonNull(strategy)) {
            repository.saveManifest(workspace.projectRoot(),
                    manifest.withReadingPreferences(strategy, manifest.nativeTextProvider()));
        }
    }
}
